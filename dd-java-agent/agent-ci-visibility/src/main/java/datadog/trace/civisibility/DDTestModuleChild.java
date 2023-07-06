package datadog.trace.civisibility;

import datadog.trace.api.Config;
import datadog.trace.api.civisibility.DDTestModule;
import datadog.trace.api.civisibility.DDTestSuite;
import datadog.trace.api.civisibility.source.SourcePathResolver;
import datadog.trace.civisibility.codeowners.Codeowners;
import datadog.trace.civisibility.context.ParentProcessTestContext;
import datadog.trace.civisibility.decorator.TestDecorator;
import datadog.trace.civisibility.ipc.ModuleExecutionResult;
import datadog.trace.civisibility.ipc.SignalClient;
import datadog.trace.civisibility.source.MethodLinesResolver;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a test module in a child process (JVM that is forked by build system to run the
 * tests)
 */
public class DDTestModuleChild implements DDTestModule {

  private static final Logger log = LoggerFactory.getLogger(DDTestModuleChild.class);

  private final String moduleName;
  private final ParentProcessTestContext context;
  private final Config config;
  private final TestDecorator testDecorator;
  private final SourcePathResolver sourcePathResolver;
  private final Codeowners codeowners;
  private final MethodLinesResolver methodLinesResolver;
  @Nullable private final InetSocketAddress signalServerAddress;

  public DDTestModuleChild(
      Long parentProcessSessionId,
      Long parentProcessModuleId,
      String moduleName,
      Config config,
      TestDecorator testDecorator,
      SourcePathResolver sourcePathResolver,
      Codeowners codeowners,
      MethodLinesResolver methodLinesResolver,
      @Nullable InetSocketAddress signalServerAddress) {
    this.moduleName = moduleName;
    this.config = config;
    this.testDecorator = testDecorator;
    this.sourcePathResolver = sourcePathResolver;
    this.codeowners = codeowners;
    this.methodLinesResolver = methodLinesResolver;
    this.signalServerAddress = signalServerAddress;
    this.context = new ParentProcessTestContext(parentProcessSessionId, parentProcessModuleId);
  }

  @Override
  public void setTag(String key, Object value) {
    throw new UnsupportedOperationException("Setting tags is not supported: " + key + ", " + value);
  }

  @Override
  public void setErrorInfo(Throwable error) {
    throw new UnsupportedOperationException("Setting error info is not supported: " + error);
  }

  @Override
  public void setSkipReason(String skipReason) {
    throw new UnsupportedOperationException("Setting skip reason is not supported: " + skipReason);
  }

  @Override
  public void end(@Nullable Long endTime, boolean testsSkipped) {
    // we have no span locally,
    // send execution result to parent process that has the span
    sendModuleExecutionResult(testsSkipped);
  }

  private void sendModuleExecutionResult(boolean testsSkipped) {
    long moduleId = context.getId();
    long sessionId = context.getParentId();
    boolean coverageEnabled = config.isCiVisibilityCodeCoverageEnabled();
    boolean itrEnabled = config.isCiVisibilityItrEnabled();
    ModuleExecutionResult moduleExecutionResult =
        new ModuleExecutionResult(sessionId, moduleId, coverageEnabled, itrEnabled, testsSkipped);

    try (SignalClient signalClient = new SignalClient(signalServerAddress)) {
      signalClient.send(moduleExecutionResult);
    } catch (Exception e) {
      log.error("Error while reporting module execution result", e);
    }
  }

  @Override
  public DDTestSuite testSuiteStart(
      String testSuiteName,
      @Nullable Class<?> testClass,
      @Nullable Long startTime,
      boolean parallelized) {
    return new DDTestSuiteImpl(
        context,
        moduleName,
        testSuiteName,
        testClass,
        startTime,
        config,
        testDecorator,
        sourcePathResolver,
        codeowners,
        methodLinesResolver,
        parallelized);
  }
}
