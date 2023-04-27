package de.daniel.marlinghaus.vidr.incompatibility.checker;

import static de.daniel.marlinghaus.vidr.incompatibility.type.GeneralIncompatibilityType.BINARY_INCOMPATIBILITY;

import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;
import java.nio.file.ClosedFileSystemException;
import java.util.Set;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Multimaps;
import org.gradle.api.GradleException;
import sootup.callgraph.CallGraph;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.core.model.SootClassMember;
import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.ViewTypeHierarchy;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class StaticBytecodeChecker extends AbstractIncompatibilityChecker {

  private MutableMultimap<String, IncompatibilityDependency> duplicateClassesDi;
  private MutableSet<MethodSignature> referencedRootProjectFeatureSetRH;
  private MutableMultimap<String, JavaSootMethod> referencedDuplicateClassFeatureSetRDi;
  private MutableMultimap<String, JavaSootMethod> referencedFeatureSetRi;
  private MutableMultimap<String, JavaSootMethod> loadedFeatureSetLi;
  private MutableMultimap<String, JavaSootMethod> shadowedFeatureSetSi;

  private JavaView rootProjectView;
  private ImmutableList<JavaSootClass> rootProjectClasses;
  private MutableSet<MethodSignature> rootProjectRiskMethodSet;

  @Override
  protected IncompatibilityDependencyCheckResult doCheck(IncompatibilityDependency dependency) {
    log.quiet("Do static bytecode check");

    if (dependency.isRootProject()) {
      //define sets
      init();
      rootProjectView = dependency.getByteCode().createOnDemandView();
      rootProjectClasses = Lists.immutable.ofAll(
          dependency.getByteCode().createOnDemandView().getClasses());

      defineDuplicateSets(dependency, rootProjectClasses);
      log.info("Duplicate classes (Di): {}", duplicateClassesDi);
      log.info("Referenced duplicate class feature set (RDi): {}",
          referencedDuplicateClassFeatureSetRDi);
      log.info("Loaded feature set (Li): {}", loadedFeatureSetLi);
      log.info("Shadowed feature set (Si): {}", shadowedFeatureSetSi);

      defineReferencedRootProjectFeatureSet();
      log.info("Referenced root project feature set (RH): {}", referencedRootProjectFeatureSetRH);

      defineReferencedFeatureSet();
      log.info("Referenced feature set (Ri): {}", referencedFeatureSetRi);

    }

    MutableMultimap<String, JavaSootMethod> incompatibilityMethods = Multimaps.mutable.set.empty();
    MutableMultimap<String, IncompatibilityDependencyCheckResult> incompatibilities = Multimaps.mutable.set.empty();
    //check
    loadedFeatureSetLi.forEachKeyMultiValues((classSignature, li) -> {
      MutableCollection<JavaSootMethod> ri = referencedFeatureSetRi.get(classSignature);

      //TODO refer to individual riskLevels with ri c li && ri c si
      if (!li.containsAll(ri)) {
        incompatibilityMethods.putAll(classSignature, ri);
        duplicateClassesDi.get(classSignature).forEach(d -> {
          var checkResult = IncompatibilityDependencyCheckResult.builder()
              .incompatible(true).type(BINARY_INCOMPATIBILITY).riskLevel(4)
              .name(d.getName()).group(d.getGroup()).version(d.getVersion())
              .build();
          incompatibilities.put(classSignature, checkResult);
        });
      }
    });
    log.quiet("Incompatibilities (Ii): {}", incompatibilities);
    log.quiet("--Successful--\n");

    //TODO implement
    return IncompatibilityDependencyCheckResult.builder()
        .build();
  }


  private void defineReferencedFeatureSet() {
    CallGraphAlgorithm callGraphAlgorithm = new ClassHierarchyAnalysisAlgorithm(rootProjectView,
        new ViewTypeHierarchy(rootProjectView));
    CallGraph callGraph = callGraphAlgorithm.initialize(referencedRootProjectFeatureSetRH.toList());

    referencedDuplicateClassFeatureSetRDi.forEachKeyMultiValues(
        (classSignature, rdi) -> rdi.forEach(duplicateClassMethod -> {
          //get used dependency methods by rootMethod calls
          Set<MethodSignature> duplicateClassMethodCallers = callGraph.callsTo(
              duplicateClassMethod.getSignature());

          if (!duplicateClassMethodCallers.isEmpty()) {
            rootProjectRiskMethodSet.addAll(duplicateClassMethodCallers);
            referencedFeatureSetRi.put(classSignature, duplicateClassMethod);
          }
        }));
  }

  private void defineReferencedRootProjectFeatureSet() {
    rootProjectClasses.forEach(rClass -> referencedRootProjectFeatureSetRH.addAll(
        rClass.getMethods().stream().map(SootClassMember::getSignature).toList()));
  }

  private void defineDuplicateSets(IncompatibilityDependency parentDependency,
      ImmutableList<JavaSootClass> parentDependencyClasses) {
    parentDependency.getTransitiveDependencies().forEach(transitiveDependency -> {
      try {
        ImmutableList<JavaSootClass> tdClasses = Lists.immutable.ofAll(
            transitiveDependency.getByteCode().createOnDemandView().getClasses());
        tdClasses.forEach(tdClass -> parentDependencyClasses.detectOptional(
            pdClass -> tdClass.getName().equals(pdClass.getName())
                || duplicateClassesDi.containsKey(tdClass.getName())).ifPresent(duplicateClass -> {
          //TODO if necessary add duplicate classes to each dependencyObject
          String classSignature = duplicateClass.getName();
          Set<? extends JavaSootMethod> methods = tdClass.getMethods();

          duplicateClassesDi.put(classSignature, transitiveDependency);
          duplicateClassesDi.put(classSignature, parentDependency);

          referencedDuplicateClassFeatureSetRDi.putAll(classSignature, methods);

          if (transitiveDependency.isLoadedDependency()) {
            loadedFeatureSetLi.putAll(classSignature, methods);
          } else {
            shadowedFeatureSetSi.putAll(classSignature, methods);
          }
        }));

        defineDuplicateSets(transitiveDependency, tdClasses);
      } catch (ClosedFileSystemException e) {
        var message = String.format("Failed to define duplicate classes (Di) for %s ",
            transitiveDependency.getDependencyName());
        log.error(message);
        //Fixme analyze ClosedFileSystemException reason for some dependency jars
        throw new GradleException(message, e);
      }
    });
  }

  private void init() {
    duplicateClassesDi = Multimaps.mutable.set.empty();
    referencedRootProjectFeatureSetRH = Sets.mutable.empty();
    referencedDuplicateClassFeatureSetRDi = Multimaps.mutable.set.empty();
    referencedFeatureSetRi = Multimaps.mutable.set.empty();
    loadedFeatureSetLi = Multimaps.mutable.set.empty();
    shadowedFeatureSetSi = Multimaps.mutable.set.empty();

    rootProjectRiskMethodSet = Sets.mutable.empty();
  }
}
