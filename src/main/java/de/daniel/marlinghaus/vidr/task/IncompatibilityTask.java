package de.daniel.marlinghaus.vidr.task;

import static de.daniel.marlinghaus.vidr.utils.soot.SootUtil.LONG_TIME_LIB;

import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.utils.soot.SootUtil;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

public abstract class IncompatibilityTask extends DefaultTask {

  @Internal
  public abstract Property<ResolvedConfiguration> getResolvedConfiguration();

  @Setter
  protected ResolvableDependencies resolvableDependencies;
  @Setter
  protected String javaSourceCompatibility;
  protected IncompatibilityDependency rootProject;
  private MutableMap<String, IncompatibilityDependency> resolvableIncompatibleDependencies = Maps.mutable.empty();
//  protected ImmutableMap<String, ImmutableList<JavaSootClass>> resolvedDependencyClasses;

  /**
   * Prepares incompatibility configuration and executes specific logic
   */
  @TaskAction
  void run() {
    Project project = getProject();
    int projectJavaVersion = JavaVersion.toVersion(javaSourceCompatibility).ordinal() + 1;
    rootProject = IncompatibilityDependency.builder()
        .name(project.getName())
        .group(project.getGroup().toString())
        .version(project.getVersion().toString())
        .rootProject(true)
        .byteCode(SootUtil.configureProject(
            projectJavaVersion,
            getProject().getBuildDir().toPath(), true)).build();

    //TODO get all unresolved configuration/ not actual dependency versions to check version conflict
//    List<Path> resolvedJarFiles = getResolvedConfiguration().get().getResolvedArtifacts().stream()
//        .filter(a -> {
//          var module = a.getModuleVersion().getId().getModule();
//          return !LONG_TIME_LIB.contains(module.toString());
//        }).map(ResolvedArtifact::getFile).filter(f -> f.getName().endsWith(".jar"))
//        .map(File::toPath).toList();

    //TODO only get project for each dependency
//    resolvedDependencyClasses = SootUtil.getJarDependencyClasses(
//        resolvedJarFiles);

    for (ResolvedDependency directProjectDependency : getResolvedConfiguration().get()
        .getFirstLevelModuleDependencies()) {
      buildDependencyGraph(projectJavaVersion, directProjectDependency, rootProject);
    }

    execute();
  }

  /**
   * recursively build dependency graph from root to transitive dependencies
   *
   * @param projectJavaVersion java version of project
   * @param directDependency   direct dependency from parent point of view
   * @param parent             parent dependency (or root project)
   */
  //adapted from CycloneDxTask
  private void buildDependencyGraph(int projectJavaVersion,
      ResolvedDependency directDependency, IncompatibilityDependency parent) {

    getJarArtifact(directDependency).ifPresent(resolvedArtifact -> {
      getLogger().debug("__{}__", parent.getDependencyName());
      getLogger().debug("    ++{}  ", directDependency.getModule());

      var identifier = resolvedArtifact.getModuleVersion().getId();
      if (!LONG_TIME_LIB.contains(identifier.getModule().toString())) {
        //build direct dependency
        IncompatibilityDependency dependency = IncompatibilityDependency.builder()
            .name(identifier.getName())
            .group(identifier.getGroup())
            .version(identifier.getVersion())
            .byteCode(SootUtil.tryGetProjectForJavaVersion(resolvedArtifact.getFile().toPath(),
                projectJavaVersion))
            .transitiveProjectDependency(!parent.isRootProject())
            .build();

        //add direct dependency to parent
        parent.getTransitiveDependencies().add(dependency);

        Set<ResolvedDependency> transitiveDependencies = directDependency.getChildren();
        if (!transitiveDependencies.isEmpty()) {
          String dependencyUid = identifier.toString();
          //build transitive dependencies
          transitiveDependencies.forEach(
              transitiveDependency -> {
                if (!resolvableIncompatibleDependencies.containsKey(dependencyUid)) {
                  resolvableIncompatibleDependencies.put(dependencyUid, dependency);
                  buildDependencyGraph(projectJavaVersion, transitiveDependency, dependency);
                }
              });
          resolvableIncompatibleDependencies.remove(dependencyUid);
        }
      }
    });
  }

  private void printDependencyGraph(IncompatibilityDependency root) {
    AtomicInteger dependencyDepth = new AtomicInteger(11);
    logDependency(root, dependencyDepth);
    dependencyDepth.getAndSet(21);
    printdependencies(root, dependencyDepth);
  }

  private void printdependencies(IncompatibilityDependency root, AtomicInteger dependencyDepth) {
    root.getTransitiveDependencies().forEach(td -> {
      logDependency(td, dependencyDepth);
      if (!td.getTransitiveDependencies().isEmpty()) {
        dependencyDepth.getAndSet(dependencyDepth.addAndGet(10));
        printdependencies(td, dependencyDepth);
      }
      dependencyDepth.getAndSet(21);
    });
  }

  private void logDependency(IncompatibilityDependency root, AtomicInteger dependencyDepth) {
    String dependencyUid = String.format("%s:%s", root.getDependencyName(), root.getVersion());
    getLogger().quiet("{}", String.format("%" + dependencyDepth.get() + "s",
        dependencyUid));
  }

  //adapted from CycloneDxTask
  private Optional<ResolvedArtifact> getJarArtifact(ResolvedDependency dependency) {
    return dependency.getModuleArtifacts().stream().filter(a -> Objects.equals(a.getType(), "jar"))
        .findFirst();
  }

  abstract void execute();
}
