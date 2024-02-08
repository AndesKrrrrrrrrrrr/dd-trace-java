package datadog.trace.instrumentation.guava10;

import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activeScope;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import com.google.common.util.concurrent.AbstractFuture;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.agent.tooling.InstrumenterGroup;
import datadog.trace.bootstrap.ContextStore;
import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.java.concurrent.ExecutorInstrumentationUtils;
import datadog.trace.bootstrap.instrumentation.java.concurrent.RunnableWrapper;
import datadog.trace.bootstrap.instrumentation.java.concurrent.State;
import java.util.Map;
import java.util.concurrent.Executor;
import net.bytebuddy.asm.Advice;

@AutoService(Instrumenter.class)
public class ListenableFutureInstrumentation extends InstrumenterGroup.Tracing
    implements Instrumenter.ForSingleType {

  public ListenableFutureInstrumentation() {
    super("guava");
  }

  @Override
  public String instrumentedType() {
    return "com.google.common.util.concurrent.AbstractFuture";
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      this.packageName + ".GuavaAsyncResultSupportExtension",
    };
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap(Runnable.class.getName(), State.class.getName());
  }

  @Override
  public void methodAdvice(MethodTransformer transformer) {
    transformer.applyAdvice(
        isConstructor(), ListenableFutureInstrumentation.class.getName() + "$AbstractFutureAdvice");
    transformer.applyAdvice(
        named("addListener").and(takesArguments(Runnable.class, Executor.class)),
        ListenableFutureInstrumentation.class.getName() + "$AddListenerAdvice");
  }

  public static class AbstractFutureAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void init() {
      GuavaAsyncResultSupportExtension.initialize();
    }
  }

  public static class AddListenerAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static State addListenerEnter(
        @Advice.Argument(value = 0, readOnly = false) Runnable task,
        @Advice.Argument(1) final Executor executor) {
      final AgentScope scope = activeScope();
      final Runnable newTask = RunnableWrapper.wrapIfNeeded(task);
      // It is important to check potentially wrapped task if we can instrument task in this
      // executor. Some executors do not support wrapped tasks.
      if (ExecutorInstrumentationUtils.shouldAttachStateToTask(newTask, executor)) {
        task = newTask;
        final ContextStore<Runnable, State> contextStore =
            InstrumentationContext.get(Runnable.class, State.class);
        return ExecutorInstrumentationUtils.setupState(contextStore, newTask, scope);
      }
      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void addListenerExit(
        @Advice.Argument(1) final Executor executor,
        @Advice.Enter final State state,
        @Advice.Thrown final Throwable throwable) {
      ExecutorInstrumentationUtils.cleanUpOnMethodExit(executor, state, throwable);
    }

    private static void muzzleCheck(final AbstractFuture<?> future) {
      future.addListener(null, null);
    }
  }
}
