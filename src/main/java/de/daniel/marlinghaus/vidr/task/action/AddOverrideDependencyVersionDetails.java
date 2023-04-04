package de.daniel.marlinghaus.vidr.task.action;

import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.VulnerableDependency;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ModuleVersionSelector;

/**
 * Adapted from
 * <a href="https://github.com/project-ncl/gradle-manipulator">gradle-manipulator</a>
 * <a
 * href="https://github.com/project-ncl/gradle-manipulator/blob/main/manipulation/src/main/java/org/jboss/gm/manipulation/actions/AlignedDependencyResolverAction.java">AlignedDependencyResolverAction.java</a>
 */
@RequiredArgsConstructor
public class AddOverrideDependencyVersionDetails implements Action<DependencyResolveDetails> {

  private final VulnerableDependency dependency;

  /**
   * <a
   * href="https://docs.gradle.org/current/userguide/resolution_rules.html#sec:denying_version">See
   * denying version for details</a>
   *
   * @param dependencyResolveDetails The object to perform the action on.
   */
  @Override
  public void execute(DependencyResolveDetails dependencyResolveDetails) {
//    logger.info("Overriding dependency {} with new version {}", key, aligned);
    String overrideVersion = dependency.getFixVersion();
    ModuleVersionSelector requested = dependencyResolveDetails.getRequested();
    if (overrideVersion != null) {
      //workaround for framework spring (boot)
      if (requested.getGroup().equals(dependency.getGroup())
          && dependency.isSpringBootDependency()) {
        changeVersion(dependencyResolveDetails, overrideVersion);
      } else if (requested.getName().equals(dependency.getName())
          && requested.getGroup().equals(dependency.getGroup())) {
        changeVersion(dependencyResolveDetails, overrideVersion);
      }
    } else {
      throw new GradleException(
          this.getClass().getName() + ": override version of dependency cannot be empty.");
    }
  }

  private void changeVersion(DependencyResolveDetails dependencyResolveDetails,
      String overrideVersion) {
    dependencyResolveDetails.because(
            String.format("VIDR: %s needs to be overwritten from %s to %s due to %s",
                dependency.getDependencyName(),
                dependencyResolveDetails.getTarget().getVersion(),
                overrideVersion,
                "vulnerability"))//TODO pass reason somehow
        .useVersion(overrideVersion);
  }

}
