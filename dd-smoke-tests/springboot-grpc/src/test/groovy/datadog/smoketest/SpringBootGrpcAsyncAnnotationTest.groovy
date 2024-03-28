package datadog.smoketest

class SpringBootGrpcAsyncAnnotationTest extends SpringBootWithGRPCTest {

  private static final Set<String> EXPECTED_TRACES =
  [
    "[serialize]",
    "[grpc.server[serialize][grpc.message]]",
    "[servlet.request[spring.handler[AsyncTask.greet[grpc.client[grpc.message[deserialize]][serialize]]]]]",
    "[deserialize]",
    "[servlet.request[spring.handler[AsyncTask.greet[grpc.client[grpc.message[deserialize]]]]]]",
    "[grpc.server[grpc.message]]",
    "[servlet.request[spring.handler[AsyncTask.greet[grpc.client[grpc.message]]]]]"
  ].toSet()

  @Override
  protected Set<String> expectedTraces() {
    return EXPECTED_TRACES
  }

  @Override
  String route() {
    return "async_annotation_greeting"
  }
}
