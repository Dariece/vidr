package de.daniel.marlinghaus.vidr.incompatibility.checker;

import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependency;
import de.daniel.marlinghaus.vidr.incompatibility.vo.IncompatibilityDependencyCheckResult;
import java.nio.file.ClosedFileSystemException;
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

  private MutableMultimap<String, IncompatibilityDependency> duplicateClassesDi;
  private MutableSet<MethodSignature> referencedRootProjectFeatureSetRH;
  private MutableMap<String, Set<? extends JavaSootMethod>> referencedDuplicateClassFeatureSetRDi;
  private MutableMultimap<String, JavaSootMethod> referencedFeatureSetRi;
  private MutableMap<String, Set<? extends JavaSootMethod>> loadedFeatureSetLi;
  private MutableMap<String, Set<? extends JavaSootMethod>> shadowedFeatureSetSi;

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

      defineDuplicateClasses(dependency, rootProjectClasses);
      log.quiet("Duplicate classes (Di): {}", duplicateClassesDi);
      log.quiet("Referenced duplicate class feature set (RDi): {}",
          referencedDuplicateClassFeatureSetRDi);

      defineReferencedRootProjectFeatureSet();
      log.quiet("Referenced root project feature set (RH): {}", referencedRootProjectFeatureSetRH);

      defineReferencedFeatureSet();
      log.quiet("Referenced feature set (Ri): {}", referencedFeatureSetRi);

      defineShadowedFeatureSet();
      log.quiet("Shadowed feature set (Si): {}", shadowedFeatureSetSi);
    }

    MutableMultimap<String, JavaSootMethod> incompatibilities = Multimaps.mutable.set.empty();
    //check
    loadedFeatureSetLi.forEach((classSignature, li) -> {
      MutableCollection<JavaSootMethod> ri = referencedFeatureSetRi.get(classSignature);

      //TODO refer to individual riskLevels with ri c li && ri c si
      if (!li.containsAll(ri)) {
        incompatibilities.putAll(classSignature, ri);
      }
    });
    log.quiet("Incompatibilities (Ii): {}", incompatibilities);
    log.quiet("--Successful--\n");

    return IncompatibilityDependencyCheckResult.builder()
        .build();//TODO refer to matching dependency for result
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
      try {
        ImmutableList<JavaSootClass> tdClasses = Lists.immutable.ofAll(
            transitiveDependency.getByteCode().createOnDemandView().getClasses());
        tdClasses.forEach(tdClass -> parentDependencyClasses.detectOptional(
            pdClass -> tdClass.getName().equals(pdClass.getName())
                || duplicateClassesDi.containsKey(
                tdClass.getName())).ifPresent(duplicateClass -> {
          duplicateClassesDi.put(duplicateClass.getName(), transitiveDependency);
          duplicateClassesDi.put(duplicateClass.getName(), parentDependency);
          defineReferencedDuplicateClassFeatureSet(duplicateClass.getName(), tdClass.getMethods());
          //TODO if necessary add duplicate classes to each dependencyObject
        }));

        defineDuplicateClasses(transitiveDependency, tdClasses);
      } catch (ClosedFileSystemException e) {
        var message = String.format("Failed to define duplicate classes (Di) for %s ",
            transitiveDependency.getDependencyName());
        log.error(message);
        //Fixme analyze ClosedFileSystemException reason for some dependency jars
//        throw new GradleException(message, e);
      }
    });
  }

  private void init() {
    duplicateClassesDi = Multimaps.mutable.set.empty();
    referencedRootProjectFeatureSetRH = Sets.mutable.empty();
    referencedDuplicateClassFeatureSetRDi = Maps.mutable.empty();
    referencedFeatureSetRi = Multimaps.mutable.set.empty();
    loadedFeatureSetLi = Maps.mutable.empty();
    shadowedFeatureSetSi = Maps.mutable.empty();

    rootProjectRiskMethodSet = Sets.mutable.empty();
  }
}
