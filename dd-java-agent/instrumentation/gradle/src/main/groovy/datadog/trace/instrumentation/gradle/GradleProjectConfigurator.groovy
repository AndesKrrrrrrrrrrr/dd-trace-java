package datadog.trace.instrumentation.gradle

import datadog.trace.api.Config
import datadog.trace.api.civisibility.config.SkippableTest
import datadog.trace.api.civisibility.config.SkippableTestsSerializer
import datadog.trace.api.config.CiVisibilityConfig
import datadog.trace.bootstrap.DatadogClassLoader
import datadog.trace.util.Strings
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.jvm.Jvm
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * Applies CI Visibility configuration to instrumented Gradle projects:
 * <ul>
 *  <li>configures Java compilation tasks to use DD Javac Plugin</li>
 *  <li>configures forked test processes to run with tracer attached</li>
 * </ul>
 *
 * <p>
 * This class is written in Groovy to circumvent compile-time safety checks,
 * since some Gradle classes that are Java-specific are not available in
 * the classloader that loads this instrumentation code
 * (the classes are available in a child CL, but injecting instrumentation code there
 * is troublesome, since there seems to be no convenient place to hook into).
 *
 * <p>
 * Another reason compile-time checks would introduce unnecessary complexity is
 * that depending on the Gradle version, different calls have to be made
 * to achieve the same result (in particular, when configuring dependencies).
 */
class GradleProjectConfigurator {

  private static final Logger log = LoggerFactory.getLogger(GradleProjectConfigurator.class)

  /**
   * Each Groovy Closure in here is a separate class.
   * When adding or removing a closure, be sure to update {@link datadog.trace.instrumentation.gradle.GradleBuildListenerInstrumentation#helperClassNames()}
   */

  public static final GradleProjectConfigurator INSTANCE = new GradleProjectConfigurator()

  void configureTracer(Task task, Map<String, String> propagatedSystemProperties, Collection<SkippableTest> skippableTests) {
    List<String> jvmArgs = new ArrayList<>(task.jvmArgs != null ? task.jvmArgs : Collections.<String> emptyList())

    // propagate to child process all "dd." system properties available in current process
    for (def e : propagatedSystemProperties.entrySet()) {
      jvmArgs.add("-D" + e.key + '=' + e.value)
    }

    if (skippableTests != null && !skippableTests.isEmpty()) {
      String skippableTestsString = SkippableTestsSerializer.serialize(skippableTests)
      jvmArgs.add("-D" + Strings.propertyNameToSystemPropertyName(CiVisibilityConfig.CIVISIBILITY_SKIPPABLE_TESTS) + '=' + skippableTestsString)
    }

    def ciVisibilityDebugPort = Config.get().ciVisibilityDebugPort
    if (ciVisibilityDebugPort != null) {
      jvmArgs.add(
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="
        + ciVisibilityDebugPort)
    }

    jvmArgs.add("-javaagent:" + Config.get().ciVisibilityAgentJarFile.toPath())

    task.jvmArgs(jvmArgs)
  }

  void configureCompilerPlugin(Project project, String compilerPluginVersion) {
    def moduleName = getModuleName(project)

    def closure = { task ->
      if (!task.class.name.contains('JavaCompile')) {
        return
      }

      if (!task.hasProperty('options') || !task.options.hasProperty('compilerArgs') || !task.hasProperty('classpath')) {
        // not a JavaCompile task?
        return
      }

      if (task.options.hasProperty('fork') && task.options.fork
        && task.options.hasProperty('forkOptions') && task.options.forkOptions.executable != null) {
        // a non-standard compiler is likely to be used
        return
      }

      def ddJavacPlugin = project.configurations.detachedConfiguration(project.dependencies.create("com.datadoghq:dd-javac-plugin:$compilerPluginVersion"))
      def ddJavacPluginClient = project.configurations.detachedConfiguration(project.dependencies.create("com.datadoghq:dd-javac-plugin-client:$compilerPluginVersion"))

      task.classpath = (task.classpath ?: project.files([])) + ddJavacPluginClient.asFileTree

      if (task.options.hasProperty('annotationProcessorPath')) {
        task.options.annotationProcessorPath = (task.options.annotationProcessorPath ?: project.files([])) + ddJavacPlugin
        task.options.compilerArgs += '-Xplugin:DatadogCompilerPlugin'
      } else {
        // for legacy Gradle versions
        task.options.compilerArgs += ['-processorpath', ddJavacPlugin.asPath, '-Xplugin:DatadogCompilerPlugin']
      }

      if (moduleName != null) {
        task.options.compilerArgs += ['--add-reads', "$moduleName=ALL-UNNAMED"]
      }

      // disable compiler warnings related to annotation processing,
      // since "fail-on-warning" linters might complain about the annotation that the compiler plugin injects
      task.options.compilerArgs += '-Xlint:-processing'
    }

    if (project.tasks.respondsTo("configureEach", Closure)) {
      project.tasks.configureEach closure
    } else {
      // for legacy Gradle versions
      project.tasks.all closure
    }
  }

  private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("\\s*module\\s*((\\w|\\.)+)\\s*\\{")

  private static getModuleName(Project project) {
    def dir = project.getProjectDir().toPath()
    def moduleInfo = dir.resolve(Paths.get("src", "main", "java", "module-info.java"))

    if (Files.exists(moduleInfo)) {
      def lines = Files.lines(moduleInfo)
      for (String line : lines) {
        def m = MODULE_NAME_PATTERN.matcher(line)
        if (m.matches()) {
          return m.group(1)
        }
      }
    }
    return null
  }

  void configureJacoco(Project project) {
    def config = Config.get()
    String jacocoPluginVersion = config.getCiVisibilityJacocoPluginVersion()
    if (jacocoPluginVersion == null) {
      return
    }

    project.apply("plugin": "jacoco")
    project.jacoco.toolVersion = jacocoPluginVersion

    forEveryTestTask project, { task ->
      task.jacoco.excludeClassLoaders += [DatadogClassLoader.name]

      Collection<String> instrumentedPackages = config.ciVisibilityJacocoPluginIncludes
      if (instrumentedPackages != null && !instrumentedPackages.empty) {
        task.jacoco.includes += instrumentedPackages
      } else {
        Collection<String> excludedPackages = config.ciVisibilityJacocoPluginExcludes
        if (excludedPackages != null && !excludedPackages.empty) {
          task.jacoco.excludes += excludedPackages
        }
      }
    }
  }

  private static void forEveryTestTask(Project project, Closure closure) {
    def c = { task ->
      if (GradleUtils.isTestTask(task)) {
        closure task
      }
    }

    if (project.tasks.respondsTo("configureEach", Closure)) {
      project.tasks.configureEach c
    } else {
      // for legacy Gradle versions
      project.tasks.all c
    }
  }

  Map<Path, Collection<Task>> configureProject(Project project) {
    def config = Config.get()
    if (config.ciVisibilityCompilerPluginAutoConfigurationEnabled) {
      String compilerPluginVersion = config.getCiVisibilityCompilerPluginVersion()
      configureCompilerPlugin(project, compilerPluginVersion)
    }

    configureJacoco(project)

    Map<Path, Collection<Task>> testExecutions = new HashMap<>()
    for (Task task : project.tasks) {
      if (GradleUtils.isTestTask(task)) {
        def executable = Paths.get(getEffectiveExecutable(task))
        testExecutions.computeIfAbsent(executable, k -> new ArrayList<>()).add(task)
      }
    }
    return testExecutions
  }

  private static String getEffectiveExecutable(Task task) {
    if (task.hasProperty('javaLauncher') && task.javaLauncher.isPresent()) {
      try {
        return task.javaLauncher.get().getExecutablePath().toString()
      } catch (Exception e) {
        log.error("Could not get Java launcher for test task", e)
      }
    }
    return task.hasProperty('executable') && task.executable != null
      ? task.executable
      : Jvm.current().getJavaExecutable().getAbsolutePath()
  }
}
