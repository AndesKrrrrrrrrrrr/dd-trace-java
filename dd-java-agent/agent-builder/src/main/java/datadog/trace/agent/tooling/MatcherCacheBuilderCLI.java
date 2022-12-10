package datadog.trace.agent.tooling;

import datadog.trace.agent.tooling.bytebuddy.SharedTypePools;
import datadog.trace.agent.tooling.bytebuddy.matcher.DDElementMatchers;
import datadog.trace.agent.tooling.bytebuddy.matcher.GlobalIgnoresMatcher;
import datadog.trace.agent.tooling.bytebuddy.matcher.HierarchyMatchers;
import datadog.trace.agent.tooling.matchercache.ClassMatchers;
import datadog.trace.agent.tooling.matchercache.MatcherCacheBuilder;
import datadog.trace.agent.tooling.matchercache.MatcherCacheFileBuilder;
import datadog.trace.agent.tooling.matchercache.MatcherCacheFileBuilderParams;
import datadog.trace.agent.tooling.matchercache.classfinder.ClassFinder;
import datadog.trace.api.Pair;
import datadog.trace.api.Platform;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// called by reflection from agent-bootstrap
public final class MatcherCacheBuilderCLI {
  private static final Logger log = LoggerFactory.getLogger(MatcherCacheBuilderCLI.class);

  static {
    SharedTypePools.registerIfAbsent(SharedTypePools.simpleCache());
    HierarchyMatchers.registerIfAbsent(HierarchyMatchers.simpleChecks());
  }

  public static void run(File bootstrapFile, String agentVersion, String... args) {
    MatcherCacheFileBuilderParams params;
    try {
      params = MatcherCacheFileBuilderParams.parseArgs(args).withDDJavaTracerJar(bootstrapFile);
    } catch (IllegalArgumentException e) {
      System.err.println("Failed to parse params: " + e);
      MatcherCacheFileBuilderParams.printHelp();
      return;
    }
    boolean enableAllInstrumenters = true;
    ClassMatchers classMatchers = AllClassMatchers.create(enableAllInstrumenters);
    MatcherCacheBuilder matcherCacheBuilder =
        new MatcherCacheBuilder(classMatchers, Platform.JAVA_VERSION.major, agentVersion);

    ClassFinder classFinder = new ClassFinder();
    MatcherCacheFileBuilder matcherCacheFileBuilder =
        new MatcherCacheFileBuilder(classFinder, matcherCacheBuilder);
    matcherCacheFileBuilder.buildMatcherCacheFile(params);
  }

  private static final class AllClassMatchers implements ClassMatchers {
    public static AllClassMatchers create(boolean enableAllInstrumenters) {
      DDElementMatchers.registerAsSupplier();

      final ArrayList<Instrumenter> instrumenters = new ArrayList<>();
      Instrumenter.TransformerBuilder intrumenterCollector =
          new Instrumenter.TransformerBuilder() {
            @Override
            public void applyInstrumentation(Instrumenter.HasAdvice hasAdvice) {
              if (hasAdvice instanceof Instrumenter) {
                instrumenters.add((Instrumenter) hasAdvice);
                log.debug("Found instrumenter: " + hasAdvice.getClass());
              }
            }
          };

      ServiceLoader<Instrumenter> loader =
          ServiceLoader.load(Instrumenter.class, Instrumenter.class.getClassLoader());

      // Collect all instrumenters
      for (Instrumenter instr : loader) {
        instr.instrument(intrumenterCollector);
      }

      if (enableAllInstrumenters) {
        // Enable default instrumenters
        try {
          Field enabledField = Instrumenter.Default.class.getDeclaredField("enabled");
          enabledField.setAccessible(true);
          for (Instrumenter instr : instrumenters) {
            if (instr instanceof Instrumenter.Default) {
              try {
                Object enabled = enabledField.get(instr);
                if (Boolean.FALSE.equals(enabled)) {
                  log.info("Enabling disabled instrumentation: " + instr.getClass());
                  enabledField.setBoolean(instr, true);
                }
              } catch (IllegalAccessException e) {
                log.error("Could not enable instrumentation", e);
              }
            }
          }
        } catch (NoSuchFieldException e) {
          log.error("Could not enable instrumentations", e);
        }
      }

      return new AllClassMatchers(enableAllInstrumenters, instrumenters);
    }

    private final boolean enableAllInstrumenters;
    private final Iterable<Instrumenter> instrumenters;
    private final Map<String, Set<String>> contextStoreClassesToInstrumenters;

    private AllClassMatchers(boolean enableAllInstrumenters, Iterable<Instrumenter> instrumenters) {
      this.enableAllInstrumenters = enableAllInstrumenters;
      this.instrumenters = instrumenters;
      this.contextStoreClassesToInstrumenters = new HashMap<>();
      for (Instrumenter instr : instrumenters) {
        if (instr instanceof Instrumenter.Default) {
          Instrumenter.Default instrumenter = (Instrumenter.Default) instr;
          for (String className : instrumenter.contextStore().keySet()) {
            Set<String> classInstrumenters =
                contextStoreClassesToInstrumenters.computeIfAbsent(
                    className, key -> new HashSet<>());
            classInstrumenters.add(instrumenter.getClass().getSimpleName());
          }
        }
      }
    }

    @Override
    public boolean isGloballyIgnored(String fullClassName, boolean skipAdditionalIgnores) {
      return GlobalIgnoresMatcher.isIgnored(fullClassName, skipAdditionalIgnores);
    }

    @Override
    public String matchingInstrumenters(TypeDescription typeDescription) {
      Set<String> result = allMatchingInstrumenters(typeDescription);
      if (result.isEmpty()) {
        return null;
      }
      return result.toString();
    }

    private Set<String> allMatchingInstrumenters(TypeDescription typeDescription) {
      Set<String> result = new HashSet<>();
      for (Instrumenter instr : instrumenters) {
        String instrName = instr.getClass().getSimpleName();

        Pair<ElementMatcher<? super TypeDescription>, String> typeMatcherAndHierarchyHint =
            AgentTransformerBuilder.typeMatcher(instr, !enableAllInstrumenters);

        if (typeMatcherAndHierarchyHint != null) {
          ElementMatcher<? super TypeDescription> typeMatcher =
              typeMatcherAndHierarchyHint.getLeft();
          if (typeMatcher.matches(typeDescription)) {
            result.add(instrName);
          }
        }

        String canonicalName = typeDescription.getCanonicalName();
        Set<String> matchingInstrumenters = contextStoreClassesToInstrumenters.get(canonicalName);
        if (matchingInstrumenters != null) {
          result.addAll(matchingInstrumenters);
        }
      }
      return result;
    }
  }
}
