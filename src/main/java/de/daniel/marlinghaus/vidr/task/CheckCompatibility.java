package de.daniel.marlinghaus.vidr.task;

import de.daniel.marlinghaus.vidr.incompatibility.checker.IncompatibilityChecker;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

public abstract class CheckCompatibility extends IncompatibilityTask {

//  @Setter
//  private List<VulnerableDependency> directResolvableDependencies = Lists.mutable.empty();

  private IncompatibilityChecker actualChecker;
  private final MutableList<IncompatibilityDependencyCheckResult> dependencyResults = Lists.mutable.empty();

  /**
   * Incompatibility check after vulnerability fix
   */
  @Override
  void execute() {
    actualChecker = getStrategyDeterminer().get().determineChecker();

    //check root project
    var rootProjectResult = actualChecker.check(rootProject);

    //check direct dependencies of root project
    rootProject.getTransitiveDependencies().forEach(dependency -> {
      dependencyResults.add(actualChecker.check(dependency));
    });
  }
}
