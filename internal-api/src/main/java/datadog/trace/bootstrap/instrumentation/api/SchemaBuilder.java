package datadog.trace.bootstrap.instrumentation.api;

import java.util.List;

public interface SchemaBuilder {
  void addProperty(
      String schemaName,
      String fieldName,
      boolean isArray,
      String type,
      String description,
      String ref,
      String format,
      List<String> enumValues);

  String build();
}
