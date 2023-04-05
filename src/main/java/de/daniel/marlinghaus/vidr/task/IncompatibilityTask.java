package de.daniel.marlinghaus.vidr.task;

import static de.daniel.marlinghaus.vidr.utils.soot.SootUtil.LONG_TIME_LIB;

import de.daniel.marlinghaus.vidr.utils.soot.SootUtil;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import lombok.Setter;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;

public abstract class IncompatibilityTask extends DefaultTask {

  @Internal
  public abstract Property<ResolvedConfiguration> getResolvedConfiguration();

  @Setter
  protected ResolvableDependencies resolvableDependencies;
  @Setter
  protected String javaSourceCompatibility;
  protected JavaProject rootProject;
  protected ImmutableMap<String, ImmutableList<JavaSootClass>> resolvedDependencyClasses;

  /**
   * Prepares incompatibility configuration and executes specific logic
   */
  @TaskAction
  void run() {
    rootProject = SootUtil.configureProject(
        JavaVersion.toVersion(javaSourceCompatibility).ordinal() + 1,
        getProject().getBuildDir().toPath(), true);

    //TODO get all unresolved configuration/ not actual dependency versions
    List<Path> resolvedJarFiles = getResolvedConfiguration().get().getResolvedArtifacts().stream()
        .filter(a -> {
          var module = a.getModuleVersion().getId().getModule();
          return !LONG_TIME_LIB.contains(module.toString());
        }).map(ResolvedArtifact::getFile).filter(f -> f.getName().endsWith(".jar"))
        .map(File::toPath).toList();

    resolvedDependencyClasses = SootUtil.getJarDependencyClasses(
        resolvedJarFiles);

    getLogger().quiet("Resolved jar files: {}", resolvedDependencyClasses);

    execute();
  }

  abstract void execute();
}
