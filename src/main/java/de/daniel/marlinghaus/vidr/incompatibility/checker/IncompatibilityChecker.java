package de.daniel.marlinghaus.vidr.incompatibility.checker;

import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;

public interface IncompatibilityChecker {

  /**
   * @param dependency
   * @return
   */
  IncompatibilityDependencyCheckResult check(IncompatibilityDependency dependency);
}
