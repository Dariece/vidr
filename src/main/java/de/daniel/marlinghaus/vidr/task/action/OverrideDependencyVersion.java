package de.daniel.marlinghaus.vidr.task.action;

import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.GavVulnerableDependency;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;

/**
 * Adapted from
 * <a href="https://github.com/project-ncl/gradle-manipulator">gradle-manipulator</a>
 * <a
 * href="https://github.com/project-ncl/gradle-manipulator/blob/main/manipulation/src/main/java/org/jboss/gm/manipulation/actions/OverrideDependenciesAction.java">OverrideDependenciesAction.java</a>
 */
@RequiredArgsConstructor
public class OverrideDependencyVersion implements Action<Configuration> {

  private final GavVulnerableDependency dependency;

  /**
   * @param configuration The object to perform the action on.
   */
  @Override
  public void execute(Configuration configuration) {
    if(configuration.isCanBeResolved()) {
      var actualForcedModules = configuration.getResolutionStrategy().getForcedModules();
      configuration.getResolutionStrategy()
          .eachDependency(new AddOverrideDependencyVersionDetails(dependency));

      if (!actualForcedModules.isEmpty()) {
        actualForcedModules.add(dependency.copyFix());
        // logger.info("Replacing force override of {} with {} ", requestedGAV, aligned);
        configuration.getResolutionStrategy().setForcedModules(actualForcedModules);
      }
    }
  }
}
