package de.daniel.marlinghaus.vidr.incompatibility.checker;

import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public abstract class AbstractIncompatibilityChecker implements IncompatibilityChecker {
  protected static final Logger log = Logging.getLogger(AbstractIncompatibilityChecker.class);

  protected abstract IncompatibilityDependencyCheckResult doCheck(
      IncompatibilityDependency dependency);

  protected IncompatibilityDependencyCheckResult getVersionInformationFromDatabase(
      IncompatibilityDependency dependency) {
    return IncompatibilityDependencyCheckResult.builder().build();
  }

  @Override
  public IncompatibilityDependencyCheckResult check(IncompatibilityDependency dependency) {
    log.quiet("Check compatibility for: {}", dependency.getDependencyName());
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
