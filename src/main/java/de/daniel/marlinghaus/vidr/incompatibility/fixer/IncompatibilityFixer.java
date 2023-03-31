package de.daniel.marlinghaus.vidr.incompatibility.fixer;

import de.daniel.marlinghaus.vidr.incompatibility.type.GeneralIncompatibilityType;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;

public interface IncompatibilityFixer {
  GeneralIncompatibilityType getIncompatibilityType();

  boolean fix(IncompatibilityDependencyCheckResult checkResult);

  void rebuildProjectWithUpdatedDependencies();
}
