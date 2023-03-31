package de.daniel.marlinghaus.vidr.incompatibility.fixer;

import de.daniel.marlinghaus.vidr.incompatibility.type.GeneralIncompatibilityType;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;

public class HarmonizeVersionFixer implements IncompatibilityFixer {

  @Override
  public GeneralIncompatibilityType getIncompatibilityType() {
    return null;
  }

  @Override
  public boolean fix(IncompatibilityDependencyCheckResult checkResult) {
    return false;
  }

  @Override
  public void rebuildProjectWithUpdatedDependencies() {

  }
}
