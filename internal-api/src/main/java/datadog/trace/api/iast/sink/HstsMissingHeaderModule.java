package datadog.trace.api.iast.sink;

import datadog.trace.api.iast.IastModule.OptOut;

@OptOut
public interface HstsMissingHeaderModule extends HttpRequestEndModule {}
