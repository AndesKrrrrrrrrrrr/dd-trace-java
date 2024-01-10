[ {
  "type" : "test_session_end",
  "version" : 1,
  "content" : {
    "test_session_id" : ${content_test_session_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "junit.test_session",
    "resource" : "junit-4.10",
    "start" : ${content_start},
    "duration" : ${content_duration},
    "error" : 0,
    "metrics" : {
      "process_id" : ${content_metrics_process_id},
      "_dd.profiling.enabled" : 0,
      "_dd.trace_span_attribute_schema" : 0
    },
    "meta" : {
      "test.type" : "test",
      "os.architecture" : ${content_meta_os_architecture},
      "test.status" : "fail",
      "language" : "jvm",
      "runtime.name" : ${content_meta_runtime_name},
      "runtime.vendor" : ${content_meta_runtime_vendor},
      "env" : "none",
      "os.platform" : ${content_meta_os_platform},
      "dummy_ci_tag" : "dummy_ci_tag_value",
      "os.version" : ${content_meta_os_version},
      "library_version" : ${content_meta_library_version},
      "component" : "junit",
      "_dd.profiling.ctx" : "test",
      "span.kind" : "test_session_end",
      "runtime.version" : ${content_meta_runtime_version},
      "runtime-id" : ${content_meta_runtime_id},
      "test.command" : "junit-4.10",
      "test.framework_version" : ${content_meta_test_framework_version},
      "test.framework" : "junit4"
    }
  }
}, {
  "type" : "test_module_end",
  "version" : 1,
  "content" : {
    "test_session_id" : ${content_test_session_id},
    "test_module_id" : ${content_test_module_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "junit.test_module",
    "resource" : "junit-4.10",
    "start" : ${content_start_2},
    "duration" : ${content_duration_2},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "test.type" : "test",
      "os.architecture" : ${content_meta_os_architecture},
      "test.module" : "junit-4.10",
      "test.status" : "fail",
      "runtime.name" : ${content_meta_runtime_name},
      "runtime.vendor" : ${content_meta_runtime_vendor},
      "env" : "none",
      "os.platform" : ${content_meta_os_platform},
      "dummy_ci_tag" : "dummy_ci_tag_value",
      "os.version" : ${content_meta_os_version},
      "library_version" : ${content_meta_library_version},
      "component" : "junit",
      "span.kind" : "test_module_end",
      "runtime.version" : ${content_meta_runtime_version},
      "test.framework_version" : ${content_meta_test_framework_version},
      "test.framework" : "junit4"
    }
  }
}, {
  "type" : "test_suite_end",
  "version" : 1,
  "content" : {
    "test_session_id" : ${content_test_session_id},
    "test_module_id" : ${content_test_module_id},
    "test_suite_id" : ${content_test_suite_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "junit.test_suite",
    "resource" : "org.example.TestFailedThenSucceed",
    "start" : ${content_start_3},
    "duration" : ${content_duration_3},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "test.type" : "test",
      "os.architecture" : ${content_meta_os_architecture},
      "test.source.file" : "dummy_source_path",
      "test.module" : "junit-4.10",
      "test.status" : "fail",
      "runtime.name" : ${content_meta_runtime_name},
      "runtime.vendor" : ${content_meta_runtime_vendor},
      "env" : "none",
      "os.platform" : ${content_meta_os_platform},
      "dummy_ci_tag" : "dummy_ci_tag_value",
      "os.version" : ${content_meta_os_version},
      "library_version" : ${content_meta_library_version},
      "component" : "junit",
      "span.kind" : "test_suite_end",
      "test.suite" : "org.example.TestFailedThenSucceed",
      "runtime.version" : ${content_meta_runtime_version},
      "test.framework_version" : ${content_meta_test_framework_version},
      "test.framework" : "junit4"
    }
  }
}, {
  "type" : "test",
  "version" : 2,
  "content" : {
    "trace_id" : ${content_trace_id},
    "span_id" : ${content_span_id},
    "parent_id" : ${content_parent_id},
    "test_session_id" : ${content_test_session_id},
    "test_module_id" : ${content_test_module_id},
    "test_suite_id" : ${content_test_suite_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "junit.test",
    "resource" : "org.example.TestFailedThenSucceed.test_failed_then_succeed",
    "start" : ${content_start_4},
    "duration" : ${content_duration_4},
    "error" : 1,
    "metrics" : {
      "process_id" : ${content_metrics_process_id},
      "_dd.profiling.enabled" : 0,
      "_dd.trace_span_attribute_schema" : 0,
      "test.source.end" : 18,
      "test.source.start" : 12
    },
    "meta" : {
      "os.architecture" : ${content_meta_os_architecture},
      "test.source.file" : "dummy_source_path",
      "test.source.method" : "test_failed_then_succeed()V",
      "test.module" : "junit-4.10",
      "test.status" : "fail",
      "language" : "jvm",
      "runtime.name" : ${content_meta_runtime_name},
      "os.platform" : ${content_meta_os_platform},
      "test.codeowners" : "[\"owner1\",\"owner2\"]",
      "os.version" : ${content_meta_os_version},
      "library_version" : ${content_meta_library_version},
      "test.name" : "test_failed_then_succeed",
      "span.kind" : "test",
      "test.suite" : "org.example.TestFailedThenSucceed",
      "runtime.version" : ${content_meta_runtime_version},
      "runtime-id" : ${content_meta_runtime_id},
      "test.type" : "test",
      "runtime.vendor" : ${content_meta_runtime_vendor},
      "env" : "none",
      "dummy_ci_tag" : "dummy_ci_tag_value",
      "component" : "junit",
      "error.type" : "org.opentest4j.AssertionFailedError",
      "_dd.profiling.ctx" : "test",
      "error.message" : ${content_meta_error_message},
      "error.stack" : ${content_meta_error_stack},
      "test.framework_version" : ${content_meta_test_framework_version},
      "test.framework" : "junit4"
    }
  }
}, {
  "type" : "test",
  "version" : 2,
  "content" : {
    "trace_id" : ${content_trace_id_2},
    "span_id" : ${content_span_id_2},
    "parent_id" : ${content_parent_id},
    "test_session_id" : ${content_test_session_id},
    "test_module_id" : ${content_test_module_id},
    "test_suite_id" : ${content_test_suite_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "junit.test",
    "resource" : "org.example.TestFailedThenSucceed.test_failed_then_succeed",
    "start" : ${content_start_5},
    "duration" : ${content_duration_5},
    "error" : 1,
    "metrics" : {
      "process_id" : ${content_metrics_process_id},
      "_dd.profiling.enabled" : 0,
      "_dd.trace_span_attribute_schema" : 0,
      "test.source.end" : 18,
      "test.source.start" : 12
    },
    "meta" : {
      "os.architecture" : ${content_meta_os_architecture},
      "test.source.file" : "dummy_source_path",
      "test.source.method" : "test_failed_then_succeed()V",
      "test.module" : "junit-4.10",
      "test.status" : "fail",
      "language" : "jvm",
      "runtime.name" : ${content_meta_runtime_name},
      "os.platform" : ${content_meta_os_platform},
      "test.codeowners" : "[\"owner1\",\"owner2\"]",
      "os.version" : ${content_meta_os_version},
      "library_version" : ${content_meta_library_version},
      "test.name" : "test_failed_then_succeed",
      "span.kind" : "test",
      "test.suite" : "org.example.TestFailedThenSucceed",
      "runtime.version" : ${content_meta_runtime_version},
      "runtime-id" : ${content_meta_runtime_id},
      "test.type" : "test",
      "runtime.vendor" : ${content_meta_runtime_vendor},
      "env" : "none",
      "dummy_ci_tag" : "dummy_ci_tag_value",
      "component" : "junit",
      "error.type" : "org.opentest4j.AssertionFailedError",
      "_dd.profiling.ctx" : "test",
      "error.message" : ${content_meta_error_message},
      "error.stack" : ${content_meta_error_stack},
      "test.framework_version" : ${content_meta_test_framework_version},
      "test.framework" : "junit4"
    }
  }
}, {
  "type" : "test",
  "version" : 2,
  "content" : {
    "trace_id" : ${content_trace_id_3},
    "span_id" : ${content_span_id_3},
    "parent_id" : ${content_parent_id},
    "test_session_id" : ${content_test_session_id},
    "test_module_id" : ${content_test_module_id},
    "test_suite_id" : ${content_test_suite_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "junit.test",
    "resource" : "org.example.TestFailedThenSucceed.test_failed_then_succeed",
    "start" : ${content_start_6},
    "duration" : ${content_duration_6},
    "error" : 1,
    "metrics" : {
      "process_id" : ${content_metrics_process_id},
      "_dd.profiling.enabled" : 0,
      "_dd.trace_span_attribute_schema" : 0,
      "test.source.end" : 18,
      "test.source.start" : 12
    },
    "meta" : {
      "os.architecture" : ${content_meta_os_architecture},
      "test.source.file" : "dummy_source_path",
      "test.source.method" : "test_failed_then_succeed()V",
      "test.module" : "junit-4.10",
      "test.status" : "fail",
      "language" : "jvm",
      "runtime.name" : ${content_meta_runtime_name},
      "os.platform" : ${content_meta_os_platform},
      "test.codeowners" : "[\"owner1\",\"owner2\"]",
      "os.version" : ${content_meta_os_version},
      "library_version" : ${content_meta_library_version},
      "test.name" : "test_failed_then_succeed",
      "span.kind" : "test",
      "test.suite" : "org.example.TestFailedThenSucceed",
      "runtime.version" : ${content_meta_runtime_version},
      "runtime-id" : ${content_meta_runtime_id},
      "test.type" : "test",
      "runtime.vendor" : ${content_meta_runtime_vendor},
      "env" : "none",
      "dummy_ci_tag" : "dummy_ci_tag_value",
      "component" : "junit",
      "error.type" : "org.opentest4j.AssertionFailedError",
      "_dd.profiling.ctx" : "test",
      "error.message" : ${content_meta_error_message},
      "error.stack" : ${content_meta_error_stack},
      "test.framework_version" : ${content_meta_test_framework_version},
      "test.framework" : "junit4"
    }
  }
}, {
  "type" : "test",
  "version" : 2,
  "content" : {
    "trace_id" : ${content_trace_id_4},
    "span_id" : ${content_span_id_4},
    "parent_id" : ${content_parent_id},
    "test_session_id" : ${content_test_session_id},
    "test_module_id" : ${content_test_module_id},
    "test_suite_id" : ${content_test_suite_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "junit.test",
    "resource" : "org.example.TestFailedThenSucceed.test_failed_then_succeed",
    "start" : ${content_start_7},
    "duration" : ${content_duration_7},
    "error" : 0,
    "metrics" : {
      "process_id" : ${content_metrics_process_id},
      "_dd.profiling.enabled" : 0,
      "_dd.trace_span_attribute_schema" : 0,
      "test.source.end" : 18,
      "test.source.start" : 12
    },
    "meta" : {
      "os.architecture" : ${content_meta_os_architecture},
      "test.source.file" : "dummy_source_path",
      "test.source.method" : "test_failed_then_succeed()V",
      "test.module" : "junit-4.10",
      "test.status" : "pass",
      "language" : "jvm",
      "runtime.name" : ${content_meta_runtime_name},
      "os.platform" : ${content_meta_os_platform},
      "test.codeowners" : "[\"owner1\",\"owner2\"]",
      "os.version" : ${content_meta_os_version},
      "library_version" : ${content_meta_library_version},
      "test.name" : "test_failed_then_succeed",
      "span.kind" : "test",
      "test.suite" : "org.example.TestFailedThenSucceed",
      "runtime.version" : ${content_meta_runtime_version},
      "runtime-id" : ${content_meta_runtime_id},
      "test.type" : "test",
      "runtime.vendor" : ${content_meta_runtime_vendor},
      "env" : "none",
      "dummy_ci_tag" : "dummy_ci_tag_value",
      "component" : "junit",
      "_dd.profiling.ctx" : "test",
      "test.framework_version" : ${content_meta_test_framework_version},
      "test.framework" : "junit4"
    }
  }
}, {
  "type" : "span",
  "version" : 1,
  "content" : {
    "trace_id" : ${content_trace_id},
    "span_id" : ${content_span_id_5},
    "parent_id" : ${content_span_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "set_up",
    "resource" : "set_up",
    "start" : ${content_start_8},
    "duration" : ${content_duration_8},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "library_version" : ${content_meta_library_version},
      "env" : "none"
    }
  }
}, {
  "type" : "span",
  "version" : 1,
  "content" : {
    "trace_id" : ${content_trace_id_2},
    "span_id" : ${content_span_id_6},
    "parent_id" : ${content_span_id_2},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "set_up",
    "resource" : "set_up",
    "start" : ${content_start_9},
    "duration" : ${content_duration_9},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "library_version" : ${content_meta_library_version},
      "env" : "none"
    }
  }
}, {
  "type" : "span",
  "version" : 1,
  "content" : {
    "trace_id" : ${content_trace_id_3},
    "span_id" : ${content_span_id_7},
    "parent_id" : ${content_span_id_3},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "set_up",
    "resource" : "set_up",
    "start" : ${content_start_10},
    "duration" : ${content_duration_10},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "library_version" : ${content_meta_library_version},
      "env" : "none"
    }
  }
}, {
  "type" : "span",
  "version" : 1,
  "content" : {
    "trace_id" : ${content_trace_id_4},
    "span_id" : ${content_span_id_8},
    "parent_id" : ${content_span_id_4},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "set_up",
    "resource" : "set_up",
    "start" : ${content_start_11},
    "duration" : ${content_duration_11},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "library_version" : ${content_meta_library_version},
      "env" : "none"
    }
  }
}, {
  "type" : "span",
  "version" : 1,
  "content" : {
    "trace_id" : ${content_trace_id},
    "span_id" : ${content_span_id_9},
    "parent_id" : ${content_span_id},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "tear_down",
    "resource" : "tear_down",
    "start" : ${content_start_12},
    "duration" : ${content_duration_12},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "library_version" : ${content_meta_library_version},
      "env" : "none"
    }
  }
}, {
  "type" : "span",
  "version" : 1,
  "content" : {
    "trace_id" : ${content_trace_id_2},
    "span_id" : ${content_span_id_10},
    "parent_id" : ${content_span_id_2},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "tear_down",
    "resource" : "tear_down",
    "start" : ${content_start_13},
    "duration" : ${content_duration_13},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "library_version" : ${content_meta_library_version},
      "env" : "none"
    }
  }
}, {
  "type" : "span",
  "version" : 1,
  "content" : {
    "trace_id" : ${content_trace_id_3},
    "span_id" : ${content_span_id_11},
    "parent_id" : ${content_span_id_3},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "tear_down",
    "resource" : "tear_down",
    "start" : ${content_start_14},
    "duration" : ${content_duration_14},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "library_version" : ${content_meta_library_version},
      "env" : "none"
    }
  }
}, {
  "type" : "span",
  "version" : 1,
  "content" : {
    "trace_id" : ${content_trace_id_4},
    "span_id" : ${content_span_id_12},
    "parent_id" : ${content_span_id_4},
    "service" : "worker.org.gradle.process.internal.worker.gradleworkermain",
    "name" : "tear_down",
    "resource" : "tear_down",
    "start" : ${content_start_15},
    "duration" : ${content_duration_15},
    "error" : 0,
    "metrics" : { },
    "meta" : {
      "library_version" : ${content_meta_library_version},
      "env" : "none"
    }
  }
} ]