package datadog.telemetry

import datadog.telemetry.api.AppDependenciesLoaded
import datadog.telemetry.api.AppIntegrationsChange
import datadog.telemetry.api.AppStarted
import datadog.telemetry.api.Dependency
import datadog.telemetry.api.DependencyType
import datadog.telemetry.api.GenerateMetrics
import datadog.telemetry.api.Integration
import datadog.telemetry.api.Metric
import datadog.telemetry.api.RequestType
import datadog.trace.test.util.DDSpecification
import okhttp3.Request

class TelemetryServiceSpecification extends DDSpecification {
  private static final Request REQUEST = new Request.Builder()
  .url('https://example.com').build()

  RequestBuilder requestBuilder = Mock {
    build(_ as RequestType) >> REQUEST
  }
  TelemetryHttpClient httpClient = Mock()
  TelemetryServiceImpl telemetryService = new TelemetryServiceImpl(httpClient)

  void 'addStartedRequest adds app_started event'() {
    when:
    def status = telemetryService.sendAppStarted(requestBuilder)

    then:
    1 * requestBuilder.build(RequestType.APP_STARTED, { it.requestType.is(RequestType.APP_STARTED)}) >> REQUEST
    1 * httpClient.sendRequest(REQUEST) >> RequestStatus.SUCCESS
    status == RequestStatus.SUCCESS
  }

  void 'appClosingRequests returns an app_closing event'() {
    when:
    RequestStatus status = telemetryService.sendAppClosing(requestBuilder)

    then:
    1 * requestBuilder.build(RequestType.APP_CLOSING) >> REQUEST
    1 * httpClient.sendRequest(REQUEST) >> RequestStatus.SUCCESS
    status == RequestStatus.SUCCESS
  }

  void 'added configuration pairs are reported in app_start'() {
    when:
    telemetryService.addConfiguration('my name': 'my value')
    telemetryService.sendAppStarted(requestBuilder)

    then:
    1 * requestBuilder.build(RequestType.APP_STARTED, { AppStarted p ->
      p.requestType == RequestType.APP_STARTED &&
        p.configuration.first().with {
          return it.name == 'my name' && it.value == 'my value'
        }
    }) >> REQUEST
    0 * requestBuilder._
  }

  void 'added dependencies are report in app_start'() {
    when:
    def dep = new Dependency(
      hash: 'deadbeef', name: 'dep name', version: '1.2.3', type: DependencyType.SHARED_SYSTEM_LIBRARY)
    telemetryService.addDependency(dep)
    telemetryService.sendAppStarted(requestBuilder)

    then:
    1 * requestBuilder.build(RequestType.APP_STARTED, { AppStarted p ->
      p.requestType == RequestType.APP_STARTED &&
        p.dependencies.first().with {
          return it.name == 'dep name' && it.hash == 'deadbeef' &&
            version == '1.2.3' && it.type.is(DependencyType.SHARED_SYSTEM_LIBRARY)
        }
    }) >> REQUEST
    0 * requestBuilder._
  }

  void 'added dependencies are reported in app_dependencies_loaded'() {
    when:
    def dep = new Dependency(
      hash: 'deadbeef', name: 'dep name', version: '1.2.3', type: DependencyType.SHARED_SYSTEM_LIBRARY)
    telemetryService.addDependency(dep)
    def status = telemetryService.sendDependencies(requestBuilder)

    then:
    1 * requestBuilder.build(RequestType.APP_DEPENDENCIES_LOADED, { AppDependenciesLoaded p ->
      p.requestType == RequestType.APP_DEPENDENCIES_LOADED &&
        p.dependencies.first().with {
          return it.name == 'dep name' && it.hash == 'deadbeef' &&
            version == '1.2.3' && it.type.is(DependencyType.SHARED_SYSTEM_LIBRARY)
        }
    }) >> REQUEST
    0 * requestBuilder._
    1 * httpClient.sendRequest(REQUEST) >> RequestStatus.SUCCESS
    status == RequestStatus.SUCCESS
  }

  void 'added integration is reported in app_start'() {
    def integration

    when:
    integration = new Integration(
      autoEnabled: true, compatible: true, enabled: true, name: 'my integration', version: '1.2.3')
    telemetryService.addIntegration(integration)
    telemetryService.sendAppStarted(requestBuilder)

    then:
    1 * requestBuilder.build(RequestType.APP_STARTED, { AppStarted p ->
      p.requestType == RequestType.APP_STARTED &&
        p.integrations.first().is(integration)
    }) >> REQUEST
    0 * requestBuilder._
  }

  void 'added integration is reported in app_integrations_change'() {
    def integration

    when:
    integration = new Integration(
      autoEnabled: true, compatible: true, enabled: true, name: 'my integration', version: '1.2.3')
    telemetryService.addIntegration(integration)
    def status = telemetryService.sendIntegrations(requestBuilder)

    then:
    1 * requestBuilder.build(RequestType.APP_INTEGRATIONS_CHANGE, { AppIntegrationsChange p ->
      p.requestType == RequestType.APP_INTEGRATIONS_CHANGE &&
        p.integrations.first().is(integration)
    }) >> REQUEST
    0 * requestBuilder._
    1 * httpClient.sendRequest(REQUEST) >> RequestStatus.SUCCESS
    status == RequestStatus.SUCCESS
  }

  void 'added metrics are reported in generate_metrics'() {
    def metric

    when:
    metric = new Metric(metric: 'my metric', tags: ['my tag'],
    type: Metric.TypeEnum.GAUGE, points: [[0.1, 0.2], [0.2, 0.1]])
    telemetryService.addMetric(metric)
    def status = telemetryService.sendMetrics(requestBuilder)

    then:
    1 * requestBuilder.build(RequestType.GENERATE_METRICS, { GenerateMetrics p ->
      p.requestType == RequestType.GENERATE_METRICS &&
        p.libLanguage == 'java' &&
        p.namespace == 'appsec' &&
        p.libVersion == '0.0.0' &&
        p.requestType &&
        p.series.first().is(metric)
    }) >> REQUEST
    0 * requestBuilder._
    1 * httpClient.sendRequest(REQUEST) >> RequestStatus.SUCCESS
    status == RequestStatus.SUCCESS
  }
}
