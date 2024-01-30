package datadog.telemetry.log;

import datadog.telemetry.TelemetryRunnable;
import datadog.telemetry.TelemetryService;
import datadog.telemetry.api.LogMessage;
import datadog.telemetry.api.LogMessageLevel;
import datadog.trace.api.telemetry.LogCollector;

public class LogPeriodicAction implements TelemetryRunnable.TelemetryPeriodicAction {

  /**
   * The current list of packages passed in is small, but if it kept growing and this did become a
   * performance issue then we could consider using ClassNameTrie instead (ie. use the builder to
   * create the trie and store it as a constant in LogPeriodicAction to be passed in here and used
   * as a filter)
   */
  static final String[] PACKAGE_ALLOW_LIST = {
    "datadog.", "com.datadog.", "java.", "javax.", "jakarta.", "jdk.", "sun.", "com.sun."
  };

  private static final String UNKNOWN = "<unknown>";

  @Override
  public void doIteration(TelemetryService service) {
    for (LogCollector.RawLogMessage rawLogMsg : LogCollector.get().drain()) {

      LogMessage logMessage =
          new LogMessage()
              .message(rawLogMsg.message)
              .tracerTime(rawLogMsg.timestamp)
              .count(rawLogMsg.count);

      if (rawLogMsg.logLevel != null) {
        logMessage.level(LogMessageLevel.fromString(rawLogMsg.logLevel));
      }

      if (rawLogMsg.throwable != null) {
        logMessage.stackTrace(renderStackTrace(rawLogMsg.throwable));
      }

      service.addLogMessage(logMessage);
    }
  }

  private static String renderStackTrace(Throwable t) {
    StringBuilder result = new StringBuilder();

    String name = t.getClass().getCanonicalName();
    if (name == null || name.isEmpty()) {
      result.append(UNKNOWN);
    } else {
      result.append(name);
    }

    if (isDataDogCode(t)) {
      String msg = t.getMessage();
      result.append(": ");
      if (msg == null || msg.isEmpty()) {
        result.append(UNKNOWN);
      } else {
        result.append(msg);
      }
    }
    result.append('\n');

    final StackTraceElement[] stacktrace = t.getStackTrace();
    int pendingRedacted = 0;
    if (stacktrace != null) {
      for (final StackTraceElement frame : t.getStackTrace()) {
        final String className = frame.getClassName();
        if (shouldRedactClass(className)) {
          pendingRedacted++;
        } else {
          writePendingRedacted(result, pendingRedacted);
          pendingRedacted = 0;
          result.append("  at ").append(frame).append('\n');
        }
      }
    }
    writePendingRedacted(result, pendingRedacted);

    return result.toString();
  }

  private static boolean isDataDogCode(Throwable t) {
    StackTraceElement[] stackTrace = t.getStackTrace();
    if (stackTrace == null || stackTrace.length == 0) {
      return false;
    }
    String cn = stackTrace[0].getClassName();
    if (cn.isEmpty()) {
      return false;
    }
    return cn.startsWith("datadog.") || cn.startsWith("com.datadog.");
  }

  private static boolean shouldRedactClass(final String className) {
    for (final String prefix : PACKAGE_ALLOW_LIST) {
      if (className.startsWith(prefix)) {
        return false;
      }
    }
    return true;
  }

  private static void writePendingRedacted(final StringBuilder result, final int pendingRedacted) {
    if (pendingRedacted == 1) {
      result.append("  at ").append("[redacted]\n");
    } else if (pendingRedacted > 1) {
      result.append("  at [redacted: ").append(pendingRedacted).append(" frames]\n");
    }
  }
}
