package de.daniel.marlinghaus.vidr.task;

import static de.daniel.marlinghaus.vidr.utils.soot.SootUtil.LONG_TIME_LIB;

import de.daniel.marlinghaus.vidr.incompatibility.determiner.IncompatibilityStrategyDeterminer;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.utils.soot.SootUtil;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Setter;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Maps;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

public abstract class IncompatibilityTask extends DefaultTask {

  //Shared Properties
  @Internal
  public abstract Property<ResolvedConfiguration> getResolvedConfiguration();

  @Internal
  public abstract Property<ArtifactView> getArtifactFilesBeforeFixup();

  @Internal
  public abstract Property<ResolvableDependencies> getResolvableDependencies();

  //Internal
  @Setter
  protected String javaSourceCompatibility;
  protected IncompatibilityDependency rootProject;
  private final MutableMap<String, IncompatibilityDependency> resolvableIncompatibleDependencies = Maps.mutable.empty();
  private MutableSet<ResolvedDependencyResult> notResolvedDependencyVersions;

  //Build Service
  @Internal
  public abstract Property<IncompatibilityStrategyDeterminer> getStrategyDeterminer();
//  protected ImmutableMap<String, ImmutableList<JavaSootClass>> resolvedDependencyClasses;

  /**
   * Prepares incompatibility configuration and executes specific logic
   */
  @TaskAction
  void run() {
    Project project = getProject();
    int projectJavaVersion = JavaVersion.toVersion(javaSourceCompatibility).ordinal() + 1;
    rootProject = IncompatibilityDependency.builder().name(project.getName())
        .group(project.getGroup().toString()).version(project.getVersion().toString())
        .rootProject(true)
        .byteCode(
            SootUtil.configureProject(projectJavaVersion, getProject().getBuildDir().toPath(),
                true))
//        .byteCodeBuilder(SootUtil.configureProjectBuilder(projectJavaVersion,
//            getProject().getBuildDir().toPath(),
//            true))
        .build();

    //get not actual dependency versions to check version conflict
    notResolvedDependencyVersions = Sets.mutable.empty();
    getResolvableDependencies().get().getResolutionResult().getAllComponents()
        .forEach(resolvedComponent -> {
          resolvedComponent.getDependents().forEach(availableDependencyVersion -> {
            if (!Objects.equals(resolvedComponent.getModuleVersion().toString(),
                availableDependencyVersion.getRequested().getDisplayName())) {
              notResolvedDependencyVersions.add(availableDependencyVersion);
            }
          });
        });

    for (ResolvedDependency directProjectDependency : getResolvedConfiguration().get()
        .getFirstLevelModuleDependencies()) {
      buildDependencyGraph(projectJavaVersion, directProjectDependency, rootProject);
    }

    //Fixme test only
////    MutableList<IncompatibilityDependency> dependenciesBeforeFixup = Lists.mutable.empty();
//    getArtifactFilesBeforeFixup().get().getArtifacts().forEach(
//        a -> {
//          String[] identifier = a.getVariant().getDisplayName().split(":");
//          IncompatibilityDependency dependency = IncompatibilityDependency.builder()
//              .name(identifier[1])
//              .group(identifier[0])
//              .version(identifier[2].split(" ")[0].trim())
//              .byteCode(SootUtil.tryGetProjectForJavaVersion(a.getFile().toPath(),
//                  projectJavaVersion))
//              .transitiveProjectDependency(false)
//              .build();
//          rootProject.getTransitiveDependencies().add(dependency);
////          dependenciesBeforeFixup.add(dependency);
//        }
//    );
////    getLogger().quiet("dependenciesBeforeFixup: {}", dependenciesBeforeFixup);

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
  private void buildDependencyGraph(int projectJavaVersion, ResolvedDependency directDependency,
      IncompatibilityDependency parent) {

    getJarArtifact(directDependency).ifPresent(resolvedArtifact -> {
      var identifier = resolvedArtifact.getModuleVersion().getId();
      if (!LONG_TIME_LIB.contains(identifier.getModule().toString())) {
        //build direct childDependency
        Path artifactPath = resolvedArtifact.getFile().toPath();
        IncompatibilityDependency childDependency = IncompatibilityDependency.builder()
            .name(identifier.getName()).group(identifier.getGroup())
            .version(identifier.getVersion())
            //TODO add child jar source to same soot project
            .byteCode(SootUtil.tryGetProjectForJavaVersion(artifactPath, projectJavaVersion))
            .transitiveProjectDependency(!parent.isRootProject()).build();
//        childDependency.setByteCodeBuilder(SootUtil.configureProjectBuilder(
//            childDependency.getByteCode().getLanguage().getVersion(), artifactPath, false));

        //add projectBuilder for childDependency with correct javaVersion
//        SootUtil.addByteSources(parent.getByteCodeBuilder(),
//            childDependency.getByteCode().getInputLocations().stream().findFirst()
//                .get()); //Fixme causes JavaHeap Exception
//        SootUtil.addByteSources(rootProject.getByteCodeBuilder(),
//            SootUtil.tryGetProjectForJavaVersion(artifactPath, projectJavaVersion).getInputLocations().stream().findFirst()
//                .get()); //Fixme causes ClosedFileSystemException Exception

        notResolvedDependencyVersions.detectOptional(
                notResolved -> Objects.equals(notResolved.getSelected().getModuleVersion(), identifier)
                    && notResolved.getRequested().getDisplayName().split(":").length > 2)
            .ifPresent(notResolvedDependency -> {
              IncompatibilityDependency duplicate = childDependency.copy();
              String newDependencyIdentifier = notResolvedDependency.getRequested()
                  .getDisplayName();
              getLogger().info("Resolve duplicate Dependency: {}", notResolvedDependency);
              String[] nameSplit = newDependencyIdentifier.split(":");
              duplicate.setVersion(nameSplit[2]);

              //create anonymous configuration with childDependency and resolve it to get correct artifact path
              final Dependency jar = getProject().getDependencies().create(newDependencyIdentifier);
              final Configuration jarConf = getProject().getConfigurations()
                  .detachedConfiguration(jar);

              duplicate.setByteCode(SootUtil.tryGetProjectForJavaVersion(
                  jarConf.resolve().stream().findFirst().orElseThrow().toPath(),
                  projectJavaVersion));

              parent.getTransitiveDependencies().add(duplicate);
            });

        //add direct childDependency to parent
        parent.getTransitiveDependencies().add(childDependency);

        Set<ResolvedDependency> transitiveDependencies = directDependency.getChildren();
        if (!transitiveDependencies.isEmpty()) {
          String dependencyUid = identifier.toString();
          //build transitive dependencies
          transitiveDependencies.forEach(transitiveDependency -> {
            if (!resolvableIncompatibleDependencies.containsKey(dependencyUid)) {
              resolvableIncompatibleDependencies.put(dependencyUid, childDependency);
              buildDependencyGraph(projectJavaVersion, transitiveDependency, childDependency);
            }
          });
          resolvableIncompatibleDependencies.remove(dependencyUid);
        }
        //overwrite bytCode with added transitiveDependency sources
//        parent.setByteCode(parent.getByteCodeBuilder().build()); //Fixme causes JavaHeap Exception
//        rootProject.setByteCode(rootProject.getByteCodeBuilder().build()); //Fixme causes ClosedFileSystemException Exception
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
    getLogger().quiet("{}", String.format("%" + dependencyDepth.get() + "s", dependencyUid));
  }

  //adapted from CycloneDxTask
  private Optional<ResolvedArtifact> getJarArtifact(ResolvedDependency dependency) {
    return dependency.getModuleArtifacts().stream().filter(a -> Objects.equals(a.getType(), "jar"))
        .findFirst();
  }

  abstract void execute();
}
