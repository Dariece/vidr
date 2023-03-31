package de.daniel.marlinghaus.vidr.incompatibility.checker;

import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;

public abstract class AbstractIncompatibilityChecker implements IncompatibilityChecker {

  protected abstract IncompatibilityDependencyCheckResult doCheck(
      IncompatibilityDependency dependency);

  protected IncompatibilityDependencyCheckResult getVersionInformationFromDatabase(
      IncompatibilityDependency dependency) {
    return IncompatibilityDependencyCheckResult.builder().build();
  }

  @Override
  public IncompatibilityDependencyCheckResult check(IncompatibilityDependency dependency) {
    IncompatibilityDependencyCheckResult result;
    //...
    result = getVersionInformationFromDatabase(dependency);
    if (result.getFixingVersions().isEmpty()) {
      //Check Logic
      result = doCheck(dependency);
      //...
    }

    //...
    return result;
  }
}
