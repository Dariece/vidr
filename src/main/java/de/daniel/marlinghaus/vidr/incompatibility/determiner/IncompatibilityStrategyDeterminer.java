package de.daniel.marlinghaus.vidr.incompatibility.determiner;

import de.daniel.marlinghaus.vidr.incompatibility.checker.IncompatibilityChecker;
import de.daniel.marlinghaus.vidr.incompatibility.exception.NotDetermineableException;
import de.daniel.marlinghaus.vidr.incompatibility.fixer.IncompatibilityFixer;
import de.daniel.marlinghaus.vidr.incompatibility.type.GeneralIncompatibilityType;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;
import java.util.List;

public abstract class IncompatibilityStrategyDeterminer {
  List<IncompatibilityChecker> checkStrategies;
  List<IncompatibilityFixer> fixStrategies;
  IncompatibilityFixer lastUsedFixer;
  IncompatibilityChecker lastUsedChecker;
  public abstract IncompatibilityChecker determineChecker(IncompatibilityDependency dependency);
  protected abstract IncompatibilityFixer getFixingStrategyFromDatabase(IncompatibilityDependencyCheckResult checkResult);
  protected abstract IncompatibilityFixer doDetermineFixer(GeneralIncompatibilityType type);
  public IncompatibilityFixer determineFixer(IncompatibilityDependencyCheckResult checkResult) throws NotDetermineableException {
    IncompatibilityFixer fixer;

    fixer = getFixingStrategyFromDatabase(checkResult);

    if(checkResult.getFixingVersions().isEmpty()){
      fixer = doDetermineFixer(checkResult.getType());
            //...
    }
        //...
    return fixer;
  }
}
