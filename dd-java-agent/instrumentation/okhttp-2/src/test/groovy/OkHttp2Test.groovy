import com.squareup.okhttp.Headers
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.internal.http.HttpMethod
import datadog.trace.agent.test.base.HttpClientTest
import datadog.trace.instrumentation.okhttp2.OkHttpClientDecorator
import spock.lang.Shared
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

@Timeout(5)
class OkHttp2Test extends HttpClientTest {
  @Override
  boolean useStrictTraceWrites() {
    // TODO fix this by making sure that spans get closed properly
    return false
  }

  @Shared
  def client = new OkHttpClient()

  def setupSpec() {
    client.setConnectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    client.setReadTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    client.setWriteTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
  }

  @Override
  int doRequest(String method, URI uri, Map<String, String> headers = [:], String body = "", Closure callback = null) {
    def reqBody = HttpMethod.requiresRequestBody(method) ? RequestBody.create(MediaType.parse("text/plain"), body) : null

    def request = new Request.Builder()
      .url(uri.toURL())
      .method(method, reqBody)
      .headers(Headers.of(HeadersUtil.headersToArray(headers)))
      .build()
    def response = client.newCall(request).execute()
    callback?.call()
    return response.code()
  }


  @Override
  CharSequence component() {
    return OkHttpClientDecorator.DECORATE.component()
  }

  @Override
  String expectedOperationName() {
    return "okhttp.request"
  }


  boolean testRedirects() {
    false
  }
}
