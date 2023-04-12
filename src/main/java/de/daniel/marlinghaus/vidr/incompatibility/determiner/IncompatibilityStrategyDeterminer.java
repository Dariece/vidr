package de.daniel.marlinghaus.vidr.incompatibility.determiner;

import de.daniel.marlinghaus.vidr.incompatibility.checker.IncompatibilityChecker;
import de.daniel.marlinghaus.vidr.incompatibility.checker.StaticBytecodeChecker;
import de.daniel.marlinghaus.vidr.incompatibility.determiner.IncompatibilityStrategyDeterminer.IncompatibilityStrategyParams;
import de.daniel.marlinghaus.vidr.incompatibility.exception.NotDetermineableException;
import de.daniel.marlinghaus.vidr.incompatibility.fixer.IncompatibilityFixer;
import de.daniel.marlinghaus.vidr.incompatibility.fixer.SetNewVersionFixer;
import de.daniel.marlinghaus.vidr.incompatibility.type.GeneralIncompatibilityType;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;
import java.util.List;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

public abstract class IncompatibilityStrategyDeterminer implements
    BuildService<IncompatibilityStrategyParams> {

  /**
   * Parameters that are needed to determine the Strategy.
   */
  public interface IncompatibilityStrategyParams extends BuildServiceParameters {

  }

  List<IncompatibilityChecker> checkStrategies = List.of(new StaticBytecodeChecker());
  List<IncompatibilityFixer> fixStrategies = List.of(new SetNewVersionFixer());
  IncompatibilityFixer lastUsedFixer;
  IncompatibilityChecker lastUsedChecker;

  public abstract IncompatibilityChecker determineChecker(IncompatibilityDependency dependency);

  /**
   * Determines the checker
   *
   * @return IncompatibilityChecker
   */
  public IncompatibilityChecker determineChecker() {
    //simple default logic
    if (lastUsedChecker == null) {
      lastUsedChecker = checkStrategies.get(0);
    }

    return lastUsedChecker;
  }

  protected abstract IncompatibilityFixer getFixingStrategyFromDatabase(
      IncompatibilityDependencyCheckResult checkResult);

  protected abstract IncompatibilityFixer doDetermineFixer(GeneralIncompatibilityType type);

  /**
   * Determines the fixer
   *
   * @return IncompatibilityFixer
   */
  public IncompatibilityFixer determineFixer(IncompatibilityDependencyCheckResult checkResult)
      throws NotDetermineableException {
    IncompatibilityFixer fixer;

    fixer = getFixingStrategyFromDatabase(checkResult);

    if (checkResult.getFixingVersions().isEmpty()) {
      fixer = doDetermineFixer(checkResult.getType());
      //...
    }
    //...
    return fixer;
  }
}
