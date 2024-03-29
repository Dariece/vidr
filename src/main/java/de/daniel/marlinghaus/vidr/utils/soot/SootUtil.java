package de.daniel.marlinghaus.vidr.utils.soot;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import sootup.callgraph.CallGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaProject.JavaProjectBuilder;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.language.JavaLanguage;
import sootup.java.core.views.JavaView;

public class SootUtil {

  /**
   * lib that takes a long time to get call-graph or causes java.lang.OutOfMemoryError: Java heap
   * space by getting all classes
   */
  public static final ImmutableSet<String> LONG_TIME_LIB = Sets.immutable.of(
      "org.scala-lang:scala-library", "org.clojure:clojure", "org.bouncycastle:bcprov-jdk15on");

  private static final Logger log = Logging.getLogger(SootUtil.class);

  public static JavaProject configureProject(int javaVersion, Path binaryPath,
      boolean isRootProject) {
    JavaLanguage language = new JavaLanguage(javaVersion);
    AnalysisInputLocation<JavaSootClass> inputLocation = new PathBasedAnalysisInputLocation(
        binaryPath, isRootProject ? SourceType.Application : SourceType.Library);

    //TODO add input location for any other dependency version on classpath before resolve
    JavaProject project = JavaProject.builder(language).addInputLocation(inputLocation).build();
//    log.quiet("{}:  {}", binaryPath.getFileName(), project.getSourceTypeSpecifier());
    return project;
  }

  public static JavaProjectBuilder configureProjectBuilder(int javaVersion, Path binaryPath,
      boolean isRootProject) {
    JavaLanguage language = new JavaLanguage(javaVersion);
    AnalysisInputLocation<JavaSootClass> inputLocation = new PathBasedAnalysisInputLocation(
        binaryPath, isRootProject ? SourceType.Application : SourceType.Library);

    return JavaProject.builder(language).addInputLocation(inputLocation);
  }

  public static void addByteSources(JavaProjectBuilder javaProject,
      AnalysisInputLocation<? extends JavaSootClass> inputLocation) {
    javaProject.addInputLocation((AnalysisInputLocation<JavaSootClass>) inputLocation);
  }

  public static ImmutableList<JavaSootClass> getAllProjectClasses(JavaProject javaProject) {
    return Lists.immutable.ofAll(javaProject.createOnDemandView().getClasses())
        .select(javaSootClass -> !javaSootClass.getName().startsWith("classes.java.test"));
  }

  public static ImmutableMap<String, ImmutableList<JavaSootClass>> getJarDependencyClasses(
      List<Path> resolvedJarFiles) {
    MutableMap<String, ImmutableList<JavaSootClass>> dependencyJarClasses = Maps.mutable.empty();

    //TODO get JavaVersion for each jar file
    resolvedJarFiles.forEach(path -> {
      String jarFilename = path.getFileName().toString();
      log.quiet("{}: ", jarFilename);
      ImmutableList<JavaSootClass> jarClasses = tryGetClassesForJavaVersion(path, 17);
      log.quiet("   --> Works!");
      dependencyJarClasses.put(jarFilename, jarClasses);
    });

    return dependencyJarClasses.toImmutable();
  }

  public static ImmutableList<JavaSootClass> tryGetClassesForJavaVersion(Path path,
      int javaVersion) {
    try {
      log.debug("JavaVersion: {}", javaVersion);
      JavaProject dependencyJar = configureProject(javaVersion, path, false);
      return getAllProjectClasses(dependencyJar);
    } catch (IllegalArgumentException e) {
      if (javaVersion >= 8) {
        return tryGetClassesForJavaVersion(path, --javaVersion);
      }
      throw new GradleException(
          String.format("%s: Could not resolve javaVersion for dependency jar %s",
              SootUtil.class.getName(), path), e);
    }
  }

  public static JavaProject tryGetProjectForJavaVersion(Path path,
      int javaVersion) {
    try {
      log.debug("JavaVersion: {}", javaVersion);
      JavaProject dependencyJar = configureProject(javaVersion, path, false);
      getAllProjectClasses(dependencyJar);
      return dependencyJar;
    } catch (IllegalArgumentException e) {
      if (javaVersion >= 8) {
        return tryGetProjectForJavaVersion(path, --javaVersion);
      }
      throw new GradleException(
          String.format("%s: Could not resolve javaVersion for dependency jar %s",
              SootUtil.class.getName(), path), e);
    }
  }

  public static JavaView tryGetViewForJavaVersion(Path path,
      int javaVersion) {
    try {
      log.debug("JavaVersion: {}", javaVersion);
      return configureProject(javaVersion, path, false).createFullView();
    } catch (IllegalArgumentException e) {
      if (javaVersion >= 8) {
        return tryGetViewForJavaVersion(path, --javaVersion);
      }
      throw new GradleException(
          String.format("%s: Could not resolve javaVersion for dependency jar %s",
              SootUtil.class.getName(), path), e);
    }
  }

  public static Map<JavaSootMethod, List<CallGraph>> retrieveEveryCallgraphForClass(
      JavaSootClass clazz) {
    Map<JavaSootMethod, List<CallGraph>> callGraphs = Maps.mutable.empty();
    if (!clazz.isJavaLibraryClass()) { //Calculate callgraph for JVM Classes like System is expensive
      clazz.getMethods().forEach(m -> {
//       callGraphs.put(m,new RapidTypeAnalysisAlgorithm(clazz, clazz.getType()))
      });
    }
    return callGraphs;
  }
}
