package de.daniel.marlinghaus.vidr.incompatibility.checker;

import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;
import java.util.Set;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Multimaps;
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

  private final MutableMultimap<String, IncompatibilityDependency> duplicateClassesDi = Multimaps.mutable.set.empty();
  private final MutableSet<MethodSignature> referencedRootProjectFeatureSetRH = Sets.mutable.empty();
  private final MutableMap<String, Set<? extends JavaSootMethod>> referencedDuplicateClassFeatureSetRDi = Maps.mutable.empty();
  private final MutableMultimap<String, JavaSootMethod> referencedFeatureSetRi = Multimaps.mutable.set.empty();
  private final MutableMap<String, Set<? extends JavaSootMethod>> loadedFeatureSetLi = Maps.mutable.empty();
  private final MutableMap<String, Set<? extends JavaSootMethod>> shadowedFeatureSetSi = Maps.mutable.empty();

  private JavaView rootProjectView;
  private ImmutableList<JavaSootClass> rootProjectClasses;
  private final MutableSet<MethodSignature> rootProjectRiskMethodSet = Sets.mutable.empty();


  @Override
  protected IncompatibilityDependencyCheckResult doCheck(IncompatibilityDependency dependency) {
    if (dependency.isRootProject()) {
      //define sets
      rootProjectView = dependency.getByteCode().createOnDemandView();
      rootProjectClasses = Lists.immutable.ofAll(
          dependency.getByteCode().createOnDemandView().getClasses());
      defineDuplicateClasses(dependency, rootProjectClasses);
      defineReferencedRootProjectFeatureSet();
      defineReferencedFeatureSet();

      defineShadowedFeatureSet();
    }


    //TODO refer to matching dependency for result
    MutableMultimap<String, JavaSootMethod> incompatibilities = Multimaps.mutable.set.empty();
    //check
    loadedFeatureSetLi.forEach((classSignature, li) -> {
      MutableCollection<JavaSootMethod> ri = referencedFeatureSetRi.get(classSignature);

      //TODO refer to individual riskLevels with ri c li && ri c si
      if (!li.containsAll(ri)) {
        incompatibilities.putAll(classSignature, ri);
      }
    });

    return IncompatibilityDependencyCheckResult.builder().build();
  }

  private void defineShadowedFeatureSet() {
    referencedDuplicateClassFeatureSetRDi.forEachKeyValue((classSignature, rdi) -> {
      if (!rdi.containsAll(loadedFeatureSetLi.get(classSignature))) {
        shadowedFeatureSetSi.put(classSignature, rdi);
      } else {//fixme fake
        loadedFeatureSetLi.put(classSignature, rdi); //TODO define loaded feature set
      }
    });
  }

  private void defineReferencedFeatureSet() {
    CallGraphAlgorithm callGraphAlgorithm = new ClassHierarchyAnalysisAlgorithm(rootProjectView,
        new ViewTypeHierarchy(rootProjectView));

    CallGraph callGraph = callGraphAlgorithm.initialize(referencedRootProjectFeatureSetRH.toList());

    referencedDuplicateClassFeatureSetRDi.forEach(
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

  private void defineReferencedDuplicateClassFeatureSet(String name,
      Set<? extends JavaSootMethod> methods) {
    referencedDuplicateClassFeatureSetRDi.put(name, methods);
  }

  private void defineReferencedRootProjectFeatureSet() {
    rootProjectClasses.forEach(rClass -> {
      referencedRootProjectFeatureSetRH.addAll(
          rClass.getMethods().stream().map(SootClassMember::getSignature).toList());
    });
  }

  private void defineDuplicateClasses(IncompatibilityDependency parentDependency,
      ImmutableList<JavaSootClass> parentDependencyClasses) {
    parentDependency.getTransitiveDependencies().forEach(transitiveDependency -> {
      ImmutableList<JavaSootClass> tdClasses = Lists.immutable.ofAll(
          transitiveDependency.getByteCode().createOnDemandView().getClasses());
      tdClasses.forEach(tdClass -> parentDependencyClasses.detectOptional(
          pdClass -> tdClass.getName().equals(pdClass.getName()) || duplicateClassesDi.containsKey(
              tdClass.getName())).ifPresent(duplicateClass -> {
        duplicateClassesDi.put(duplicateClass.getName(), transitiveDependency);
        duplicateClassesDi.put(duplicateClass.getName(), parentDependency);
        defineReferencedDuplicateClassFeatureSet(duplicateClass.getName(), tdClass.getMethods());
        //TODO if necessary add duplicate classes to each dependencyObject
      }));

      defineDuplicateClasses(transitiveDependency, tdClasses);
    });
  }
}
