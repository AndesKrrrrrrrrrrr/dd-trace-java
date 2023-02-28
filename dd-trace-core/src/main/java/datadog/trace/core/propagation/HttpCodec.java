package datadog.trace.core.propagation;

import datadog.trace.api.Config;
import datadog.trace.api.TracePropagationStyle;
import datadog.trace.bootstrap.instrumentation.api.AgentPropagation;
import datadog.trace.bootstrap.instrumentation.api.TagContext;
import datadog.trace.core.DDSpanContext;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpCodec {

  private static final Logger log = LoggerFactory.getLogger(HttpCodec.class);
  // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Forwarded
  static final String FORWARDED_KEY = "forwarded";
  static final String FORWARDED_FOR_KEY = "forwarded-for";
  static final String X_FORWARDED_PROTO_KEY = "x-forwarded-proto";
  static final String X_FORWARDED_HOST_KEY = "x-forwarded-host";
  static final String X_FORWARDED_KEY = "x-forwarded";
  static final String X_FORWARDED_FOR_KEY = "x-forwarded-for";
  static final String X_FORWARDED_PORT_KEY = "x-forwarded-port";

  // other headers which may contain real ip
  static final String CLIENT_IP_KEY = "client-ip";
  static final String TRUE_CLIENT_IP_KEY = "true-client-ip";
  static final String X_CLUSTER_CLIENT_IP_KEY = "x-cluster-client-ip";
  static final String X_REAL_IP_KEY = "x-real-ip";
  static final String USER_AGENT_KEY = "user-agent";
  static final String VIA_KEY = "via";

  public interface Injector {
    <C> void inject(
        final DDSpanContext context, final C carrier, final AgentPropagation.Setter<C> setter);
  }

  public interface Extractor {
    <C> TagContext extract(final C carrier, final AgentPropagation.ContextVisitor<C> getter);
  }

  public static Injector createInjector(
      Set<TracePropagationStyle> styles, Map<String, String> invertedBaggageMapping) {
    ArrayList<Injector> injectors =
        new ArrayList<>(createInjectors(styles, invertedBaggageMapping).values());
    return new CompoundInjector(injectors);
  }

  public static Map<TracePropagationStyle, Injector> allInjectorsFor(
      Map<String, String> reverseBaggageMapping) {
    return createInjectors(EnumSet.allOf(TracePropagationStyle.class), reverseBaggageMapping);
  }

  private static Map<TracePropagationStyle, Injector> createInjectors(
      Set<TracePropagationStyle> propagationStyles, Map<String, String> reverseBaggageMapping) {
    EnumMap<TracePropagationStyle, Injector> result = new EnumMap<>(TracePropagationStyle.class);
    for (TracePropagationStyle style : propagationStyles) {
      switch (style) {
        case DATADOG:
          result.put(style, DatadogHttpCodec.newInjector(reverseBaggageMapping));
          break;
        case B3SINGLE:
          result.put(style, B3HttpCodec.SINGLE_INJECTOR);
          break;
        case B3MULTI:
          result.put(style, B3HttpCodec.MULTI_INJECTOR);
          break;
        case HAYSTACK:
          result.put(style, HaystackHttpCodec.newInjector(reverseBaggageMapping));
          break;
        case XRAY:
          result.put(style, XRayHttpCodec.newInjector(reverseBaggageMapping));
          break;
        case NONE:
          result.put(style, NoneCodec.INJECTOR);
          break;
        default:
          log.debug("No implementation found to inject propagation style: {}", style);
          break;
      }
    }
    return result;
  }

  public static Extractor createExtractor(
      final Config config,
      final Map<String, String> taggedHeaders,
      final Map<String, String> baggageMapping) {
    final List<Extractor> extractors = new ArrayList<>();
    for (final TracePropagationStyle style : config.getTracePropagationStylesToExtract()) {
      switch (style) {
        case DATADOG:
          extractors.add(DatadogHttpCodec.newExtractor(taggedHeaders, baggageMapping, config));
          break;
        case B3SINGLE:
          extractors.add(B3HttpCodec.newSingleExtractor(taggedHeaders, baggageMapping, config));
          break;
        case B3MULTI:
          extractors.add(B3HttpCodec.newMultiExtractor(taggedHeaders, baggageMapping, config));
          break;
        case HAYSTACK:
          extractors.add(HaystackHttpCodec.newExtractor(taggedHeaders, baggageMapping));
          break;
        case XRAY:
          extractors.add(XRayHttpCodec.newExtractor(taggedHeaders, baggageMapping));
          break;
        case NONE:
          extractors.add(NoneCodec.EXTRACTOR);
          break;
        default:
          log.debug("No implementation found to extract propagation style: {}", style);
          break;
      }
    }
    return new CompoundExtractor(extractors);
  }

  public static class CompoundInjector implements Injector {

    private final List<Injector> injectors;

    public CompoundInjector(final List<Injector> injectors) {
      this.injectors = injectors;
    }

    @Override
    public <C> void inject(
        final DDSpanContext context, final C carrier, final AgentPropagation.Setter<C> setter) {
      for (final Injector injector : injectors) {
        injector.inject(context, carrier, setter);
      }
    }
  }

  public static class CompoundExtractor implements Extractor {
    private final List<Extractor> extractors;

    public CompoundExtractor(final List<Extractor> extractors) {
      this.extractors = extractors;
    }

    @Override
    public <C> TagContext extract(
        final C carrier, final AgentPropagation.ContextVisitor<C> getter) {
      TagContext context = null;

      for (final Extractor extractor : extractors) {
        context = extractor.extract(carrier, getter);
        // Use incomplete TagContext only as last resort
        if (context instanceof ExtractedContext) {
          return context;
        }
      }

      return context;
    }
  }

  /** URL encode value */
  static String encode(final String value) {
    String encoded = value;
    try {
      encoded = URLEncoder.encode(value, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      log.debug("Failed to encode value - {}", value);
    }
    return encoded;
  }

  /** URL decode value */
  static String decode(final String value) {
    String decoded = value;
    try {
      decoded = URLDecoder.decode(value, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      log.debug("Failed to decode value - {}", value);
    }
    return decoded;
  }

  static String firstHeaderValue(final String value) {
    if (value == null) {
      return null;
    }

    int firstComma = value.indexOf(',');
    return firstComma == -1 ? value : value.substring(0, firstComma).trim();
  }
}
