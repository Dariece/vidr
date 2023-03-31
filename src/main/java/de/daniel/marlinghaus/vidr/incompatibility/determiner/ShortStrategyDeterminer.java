package de.daniel.marlinghaus.vidr.incompatibility.determiner;

import de.daniel.marlinghaus.vidr.incompatibility.checker.IncompatibilityChecker;
import de.daniel.marlinghaus.vidr.incompatibility.fixer.IncompatibilityFixer;
import de.daniel.marlinghaus.vidr.incompatibility.type.GeneralIncompatibilityType;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;

public class ShortStrategyDeterminer extends IncompatibilityStrategyDeterminer {

  /**
   * @param dependency
   * @return
   */
  @Override
  public IncompatibilityChecker determineChecker(IncompatibilityDependency dependency) {
    return null;
  }

  /**
   * @param checkResult
   * @return
   */
  @Override
  protected IncompatibilityFixer getFixingStrategyFromDatabase(
      IncompatibilityDependencyCheckResult checkResult) {
    return null;
  }

  /**
   * @param type
   * @return
   */
  @Override
  protected IncompatibilityFixer doDetermineFixer(GeneralIncompatibilityType type) {
    return null;
  }
}
