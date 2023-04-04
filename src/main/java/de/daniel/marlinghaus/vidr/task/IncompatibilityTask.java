package de.daniel.marlinghaus.vidr.task;

import de.daniel.marlinghaus.vidr.utils.soot.SootUtil;
import lombok.Setter;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.tasks.TaskAction;
import sootup.java.core.JavaProject;

public abstract class IncompatibilityTask extends DefaultTask {

  @Setter
  protected ResolvedConfiguration resolvedConfiguration;
  @Setter
  protected ResolvableDependencies resolvableDependencies;
  @Setter
  protected String javaSourceCompatibility;
  protected JavaProject javaProject;

  /**
   * Prepares incompatibility configuration and executes specific logic
   */
  @TaskAction
  void run() {
    javaProject = SootUtil.configureProject(
        JavaVersion.toVersion(javaSourceCompatibility).ordinal() + 1,
        getProject().getBuildDir().toPath(), true);

    execute();
  }

  abstract void execute();
}
