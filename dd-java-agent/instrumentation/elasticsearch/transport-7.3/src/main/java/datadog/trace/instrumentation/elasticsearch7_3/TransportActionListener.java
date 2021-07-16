package datadog.trace.instrumentation.elasticsearch7_3;

import static datadog.trace.instrumentation.elasticsearch.ElasticsearchTransportClientDecorator.DECORATE;

import datadog.trace.bootstrap.InstrumentationContext;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import datadog.trace.bootstrap.instrumentation.java.concurrent.AdviceUtils;
import datadog.trace.bootstrap.instrumentation.java.concurrent.State;
import datadog.trace.context.TraceScope;
import datadog.trace.util.Strings;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.bulk.BulkShardResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.action.support.nodes.BaseNodesResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;

/** This class is identical to version 6's instrumentation. */
public class TransportActionListener<T extends ActionResponse> implements ActionListener<T> {

  private final ActionListener<T> listener;
  private final AgentSpan span;

  public TransportActionListener(
      final ActionRequest actionRequest, final ActionListener<T> listener, final AgentSpan span) {
    this.listener = listener;
    this.span = span;
    onRequest(actionRequest);
  }

  private void onRequest(final ActionRequest request) {
    try {
    if (request instanceof IndicesRequest) {
      final IndicesRequest req = (IndicesRequest) request;
      if (req.indices() != null) {
        span.setTag("elasticsearch.request.indices", Strings.join(",", req.indices()));
      }
    }
    if (request instanceof SearchRequest) {
      final SearchRequest req = (SearchRequest) request;
      span.setTag("elasticsearch.request.search.types", Strings.join(",", req.types()));
    }
    if (request instanceof DocWriteRequest) {
      final DocWriteRequest req = (DocWriteRequest) request;
      span.setTag("elasticsearch.request.write.type", req.type());
      span.setTag("elasticsearch.request.write.routing", req.routing());
      span.setTag("elasticsearch.request.write.version", req.version());
    }

    AdviceUtils.capture(InstrumentationContext.get(ActionListener.class, State.class), this, true);
  } catch (Throwable t) {
    t.printStackTrace();
  }
  }

  @Override
  public void onResponse(final T response) {
    TraceScope scope =
        AdviceUtils.startTaskScope(
            InstrumentationContext.get(ActionListener.class, State.class), this);

    if (response.remoteAddress() != null) {
      DECORATE.onPeerConnection(span, response.remoteAddress().address());
    }

    if (response instanceof GetResponse) {
      final GetResponse resp = (GetResponse) response;
      span.setTag("elasticsearch.type", resp.getType());
      span.setTag("elasticsearch.id", resp.getId());
      span.setTag("elasticsearch.version", resp.getVersion());
    }

    if (response instanceof BroadcastResponse) {
      final BroadcastResponse resp = (BroadcastResponse) response;
      span.setTag("elasticsearch.shard.broadcast.total", resp.getTotalShards());
      span.setTag("elasticsearch.shard.broadcast.successful", resp.getSuccessfulShards());
      span.setTag("elasticsearch.shard.broadcast.failed", resp.getFailedShards());
    }

    if (response instanceof ReplicationResponse) {
      final ReplicationResponse resp = (ReplicationResponse) response;
      span.setTag("elasticsearch.shard.replication.total", resp.getShardInfo().getTotal());
      span.setTag(
          "elasticsearch.shard.replication.successful", resp.getShardInfo().getSuccessful());
      span.setTag("elasticsearch.shard.replication.failed", resp.getShardInfo().getFailed());
    }

    if (response instanceof IndexResponse) {
      span.setTag("elasticsearch.response.status", ((IndexResponse) response).status().getStatus());
    }

    if (response instanceof BulkShardResponse) {
      final BulkShardResponse resp = (BulkShardResponse) response;
      span.setTag("elasticsearch.shard.bulk.id", resp.getShardId().getId());
      span.setTag("elasticsearch.shard.bulk.index", resp.getShardId().getIndexName());
    }

    if (response instanceof BaseNodesResponse) {
      final BaseNodesResponse resp = (BaseNodesResponse) response;
      if (resp.hasFailures()) {
        span.setTag("elasticsearch.node.failures", resp.failures().size());
      }
      span.setTag("elasticsearch.node.cluster.name", resp.getClusterName().value());
    }

    try {
      listener.onResponse(response);
    } finally {
      DECORATE.beforeFinish(span);
      AdviceUtils.endTaskScope(scope);
      span.finish();
    }
  }

  @Override
  public void onFailure(final Exception e) {
    TraceScope scope =
        AdviceUtils.startTaskScope(
            InstrumentationContext.get(ActionListener.class, State.class), this);

    DECORATE.onError(span, e);

    try {
      listener.onFailure(e);
    } finally {
      DECORATE.beforeFinish(span);
      AdviceUtils.endTaskScope(scope);
      span.finish();
    }
  }
}
