package datadog.trace.instrumentation.protobuf_java;

import com.google.auto.service.AutoService;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.MessageLite;
import datadog.trace.agent.tooling.Instrumenter;
import datadog.trace.agent.tooling.InstrumenterModule;
import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers.extendsClass;
import static datadog.trace.agent.tooling.bytebuddy.matcher.NameMatchers.named;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

@AutoService(Instrumenter.class)
public final class DynamicMessageInstrumentation extends InstrumenterModule.Tracing
    implements Instrumenter.ForSingleType {

  static final String instrumentationName = "protobuf";
  static final String TARGET_TYPE = "com.google.protobuf.DynamicMessage";
  static final String DESERIALIZE = "deserialize";

  public DynamicMessageInstrumentation() {
    super(instrumentationName);
  }

  @Override
  public String instrumentedType() {
    return TARGET_TYPE;
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      packageName + ".OpenAPIFormatExtractor",
    };
  }

  @Override
  public void methodAdvice(MethodTransformer transformer) {
    transformer.applyAdvice(
        isMethod().and(named("parseFrom"))
            .and(isStatic())
            .and(takesArgument(0, Descriptor.class))
            .and(returns(DynamicMessage.class)),
        DynamicMessageInstrumentation.class.getName() + "$ParseFromAdvice");
  }

  public static class ParseFromAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static AgentScope onEnter(
        @Advice.Argument(0) final Descriptor descriptor
    ) {
      final AgentSpan span = startSpan(instrumentationName, DESERIALIZE);
      OpenAPIFormatExtractor.attachSchemaOnSpan(descriptor, span, "deserialization");
      return activateSpan(span);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Enter final AgentScope scope,
        @Advice.Thrown final Throwable throwable) {
      if (scope == null) {
        return;
      }
      AgentSpan span = scope.span();
      scope.close();
      span.finish();
    }
  }
}
