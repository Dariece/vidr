package de.daniel.marlinghaus.vidr.incompatibility;

import de.daniel.marlinghaus.vidr.incompatibility.checker.IncompatibilityChecker;
import de.daniel.marlinghaus.vidr.incompatibility.determiner.IncompatibilityStrategyDeterminer;
import de.daniel.marlinghaus.vidr.incompatibility.exception.NotDetermineableException;
import de.daniel.marlinghaus.vidr.incompatibility.fixer.IncompatibilityFixer;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IncompatibilityResolver {

  private final IncompatibilityStrategyDeterminer determiner;
  private IncompatibilityChecker checker;
  private IncompatibilityFixer fixer;

  //TODO implement, still pseudocode
  public void resolve(List<IncompatibilityDependency> dependencies, Boolean isRebuild)
      throws Exception {
    boolean resolvable = false;
    List<IncompatibilityDependency> fixedDependencies = new ArrayList<>();
    List<IncompatibilityDependencyCheckResult> unresolvedDependencies = new ArrayList<>();

    for (IncompatibilityDependency dependency : dependencies) {
      IncompatibilityDependencyCheckResult checkResult;
      boolean checkDone = false;

      do {
        checker = determiner.determineChecker(dependency);

        if (checker == null) {
          break;
        }
        checkResult = checker.check(dependency);

        if (checkResult.isIncompatible()) {
          checkDone = true;
          boolean fixDone = false;

          do {
            try {
              fixer = determiner.determineFixer(checkResult);
              if (fixer.fix(checkResult)) {
                fixDone = true;
                dependency.setFixed(true);
                fixedDependencies.add(dependency);
              }
            } catch (NotDetermineableException e) {
              unresolvedDependencies.add(checkResult);
              fixDone = true;
            }
            //...
          } while (!fixDone);
        }
        //...
      } while (!checkDone);
    }

    if (!fixedDependencies.isEmpty() && unresolvedDependencies.isEmpty() && !isRebuild) {
      fixer.rebuildProjectWithUpdatedDependencies();
      resolve(fixedDependencies, true);
    }

    if (unresolvedDependencies.isEmpty()) {
      generateFixReport(fixedDependencies);
    } else {
      generateBugreport(unresolvedDependencies, isRebuild);
//      throw new SomeRuntimeException(unresolvedDependencies);
    }
  }

  private void generateBugreport(List<IncompatibilityDependencyCheckResult> unresolvedDependencies,
      Boolean isRebuild) {

  }

  private void generateFixReport(List<IncompatibilityDependency> fixedDependencies) {
  }
  //...

}
