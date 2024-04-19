package datadog.trace.agent.jfrmetrics;

import datadog.trace.api.Config;
import datadog.trace.api.StatsDClient;
import datadog.trace.api.StatsDClientManager;
import datadog.trace.bootstrap.config.provider.ConfigProvider;
import java.time.Duration;
import jdk.jfr.consumer.RecordingStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Used from datadog.trace.bootstrap.Agent
public final class JfrMetricsEmitter {
  private static final Logger log = LoggerFactory.getLogger(JfrMetricsEmitter.class);

  private static JfrMetricsEmitter instance;

  private final StatsDClient statsd;

  public static synchronized void run(StatsDClientManager statsdManager) {
    if (instance != null) {
      return;
    }

    //    log.info("Starting JFR metrics collection");
    Config cfg = Config.get();

    String host = cfg.getJmxFetchStatsdHost();
    Integer port = cfg.getJmxFetchStatsdPort();
    String namedPipe = cfg.getDogStatsDNamedPipe();

    instance =
        new JfrMetricsEmitter(
            statsdManager.statsDClient(host, port, namedPipe, null, null),
            ConfigProvider.getInstance());
    Thread t = new Thread(instance::run, "JfrMetricsComputer");
    t.start();
  }

  private final boolean enabled;
  private final int periodSeconds;

  JfrMetricsEmitter(StatsDClient statsd, ConfigProvider configProvider) {
    enabled =
        configProvider.getBoolean(
            JfrMetricsConfig.JFR_METRICS_ENABLED, JfrMetricsConfig.JFR_METRICS_ENABLED_DEFAULT);
    periodSeconds =
        configProvider.getInteger(
            JfrMetricsConfig.JFR_METRICS_PERIOD_SECONDS,
            JfrMetricsConfig.JFR_METRICS_PERIOD_SECONDS_DEFAULT);
    this.statsd = statsd;
  }

  public void run() {
    if (!enabled) {
      log.info("JFR metrics collection is disabled");
      return;
    }
    try (var rs = new RecordingStream()) {
      rs.enable("jdk.VirtualThreadSubmitFailed");
      rs.enable("jdk.VirtualThreadPinned");
      rs.enable("jdk.ResidentSetSize").withPeriod(Duration.ofSeconds(periodSeconds));
      rs.enable("jdk.NativeMemoryUsageTotal").withPeriod(Duration.ofSeconds(periodSeconds));

      rs.onEvent(
          "jdk.VirtualThreadSubmitFailed",
          event -> {
            statsd.incrementCounter(
                "jvm.virtual_thread_submit_failed",
                "thread_id:" + event.getLong("javaThreadId"),
                "thread_name:" + event.getThread().getJavaName());
          });

      rs.onEvent(
          "jdk.VirtualThreadPinned",
          event -> {
            statsd.incrementCounter(
                "jvm.virtual_thread_pinned", "thread:" + event.getThread().getJavaName());
          });

      rs.onEvent(
          "jdk.ResidentSetSize",
          event -> {
            statsd.gauge("jvm.resident_set_size", event.getLong("size"));
          });

      rs.onEvent(
          "jdk.NativeMemoryUsageTotal",
          event -> {
            statsd.gauge("jvm.native_memory_committed", event.getLong("committed"));
            statsd.gauge("jvm.native_memory_reserved", event.getLong("reserved"));
          });
      rs.start();
    }
  }
}
