package de.daniel.marlinghaus.vidr.task;

import static org.eclipse.collections.api.factory.Lists.immutable;
import static org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME;

import de.daniel.marlinghaus.vidr.task.action.OverrideDependencyVersion;
import de.daniel.marlinghaus.vidr.vulnerability.report.TrivyReportDeserializer;
import de.daniel.marlinghaus.vidr.vulnerability.report.VulnerabilityReportDeserializer;
import de.daniel.marlinghaus.vidr.vulnerability.report.vo.VulnerabilityReport;
import de.daniel.marlinghaus.vidr.vulnerability.report.vo.trivy.TrivyVulnerability;
import de.daniel.marlinghaus.vidr.vulnerability.resolve.VulnerableDependencyFixVersionResolver;
import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.VulnerableDependency;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.gradle.api.DefaultTask;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.tasks.TaskAction;

/**
 * Tries to resolve the fix-versions of vulnerable dependencies from scanner report to the project
 * configuration
 */
public abstract class ResolveDependencyFix extends DefaultTask {

  //TODO resolve services dynamically by strategy
  private final VulnerabilityReportDeserializer<TrivyVulnerability> reportDeserializer = new TrivyReportDeserializer();
  private final VulnerableDependencyFixVersionResolver<TrivyReportDeserializer> dependencyFixResolver = new VulnerableDependencyFixVersionResolver(
      reportDeserializer);
  /**
   * path to the vulnerability report file
   */
  @Setter
  private Path reportFile;
  @Getter
  private ResolvedConfiguration resolvedConfiguration;
  @Getter
  private List<VulnerableDependency> directResolvableDependencies = Lists.mutable.empty();
  private List<VulnerableDependency> unresolvableDependencies = Lists.mutable.empty();

  /**
   * Tries to resolve the fix-versions of vulnerable dependencies from scanner report to the project
   * configuration
   */
  @TaskAction
  void run() {
    try {
      //just testing: prototyping make generalizable
      //get vulnerable dependencies
      getLogger().info("Start deserialze report {}", reportFile);
      VulnerabilityReport deserializedReport = reportDeserializer.deserialize(
          reportFile);
      getLogger().quiet("Successful deserialized report");

      getLogger().info("Start resolve fix versions for vulnerable dependencies");
      List<VulnerableDependency> vulnerableFixableDependencies = dependencyFixResolver.resolveFixVersions(
          deserializedReport.getVulnerabilities());
      getLogger().debug("Fixable dependencies: {}", vulnerableFixableDependencies);
      getLogger().quiet("Successful resolved fix versions for vulnerable dependencies");
      if (!dependencyFixResolver.getUnfixableVulnerabilities().isEmpty()
          || !dependencyFixResolver.getUnfixableDependencies().isEmpty()) {
        getLogger().warn("{} vulnerable dependencies are not fixable. See report for details",
            dependencyFixResolver.getUnfixableDependencies().size());
        getLogger().debug("Following vulnerabilities are not fixable: {}",
            dependencyFixResolver.getUnfixableVulnerabilities());
        getLogger().debug("Following vulnerable dependencies are not fixable: {}",
            dependencyFixResolver.getUnfixableDependencies());
      }

      resolvedConfiguration = overrideDepencyVersionToFixed(
          vulnerableFixableDependencies, new AtomicBoolean(false));

      //was wenn nicht baubar? Kann Plugin weiterlaufen? -> scheinbar nein
    } catch (IOException e) {
      getLogger().error(" {} {}", e.getCause(), e.getMessage());
      throw new GradleException("An error occurred executing " + this.getClass().getName(), e);
    }
  }

  /**
   * Recursively try to override dependency versions to fixed
   *
   * @param vulnerableFixableDependencies list of vulnerable dependencies with a fix version
   * @param isRetryable                   is the process retryable with another fix version of any
   *                                      dependency that failed to be resolved
   * @return resolved configuration with fixed dependencies
   */
  //TODO refactor
  //TODO make override reusable for fix incompatibility
  private ResolvedConfiguration overrideDepencyVersionToFixed(
      List<VulnerableDependency> vulnerableFixableDependencies, AtomicBoolean isRetryable) {
    //ändere die versionen der betroffenen dependencies auf die gefixten
    var configurationContainer = getProject().getConfigurations();
    Configuration implementationConfiguration = configurationContainer.getByName(
        RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    ResolvableDependencies resolvableDependencies = implementationConfiguration.getIncoming(); //TODO try to get each configuration and apply override action on match
    var dependencySet = resolvableDependencies.getDependencies();
    getLogger().info("implementationConfiguration dependencies: {}",
        dependencySet.stream().toList());

    getLogger().info("Change vulnerable dependency versions to fixed");
    //add changed dependencies
    AtomicInteger springBootCount = new AtomicInteger(0);
    vulnerableFixableDependencies.forEach(
        vulnerableDependency -> {
          DomainObjectSet<Dependency> matchingDependencies = dependencySet.matching(
              dependency -> vulnerableDependency.getName().equals(dependency.getName())
                  && vulnerableDependency.getGroup().equals(dependency.getGroup()));
          //workaround framework spring
          if (!matchingDependencies.isEmpty() && vulnerableDependency.isSpringBootDependency()
              && springBootCount.getAndIncrement() > 0) {
            //gemeinsame Version bei Frameworks z. B. gemeinsame Group org.springframework beachten
            //Only write substition rule once
            getLogger().warn("Spring boot version already resolved for: {}",
                vulnerableDependency.getDependencyName());
          } else if (!matchingDependencies.isEmpty()) {
            directResolvableDependencies.add(vulnerableDependency);
            getLogger().info("Match consisting of: {}", matchingDependencies.stream().toList());
            new OverrideDependencyVersion(vulnerableDependency).execute(
                implementationConfiguration);
          } else {
            //Transitive dependencies or test configurations fail
            this.unresolvableDependencies.add(vulnerableDependency);
            getLogger().error("No match for: {}",
                vulnerableDependency.getDependencyName());
            //TODO use other configuration if not found
            //TODO find direct dependency with usage later on
          }
        });

    ResolvedConfiguration resolvedConfiguration;
    //evaluate resolution before resolve dependencies
    ResolutionResult resolutionResult = resolvableDependencies.getResolutionResult();
//        getLogger().info("Resolution result: {}", resolutionResult.getAllDependencies());
    List<UnresolvedDependencyResult> unresolvedDependencyResults = (List<UnresolvedDependencyResult>) resolutionResult.getAllDependencies()
        .stream()
        .filter(r -> r instanceof UnresolvedDependencyResult).toList();
    ImmutableList<VulnerableDependency> gavVulnerableDependencies = immutable.ofAll(
        vulnerableFixableDependencies);
    if (unresolvedDependencyResults.isEmpty()) {
      //resolves and downloads configuration (dependency changes)
      resolvedConfiguration = implementationConfiguration.getResolvedConfiguration(); //TODO analyze
//      vulnerableFixableDependencies.forEach(
//          v -> getLogger().quiet("{} {}", resolvedConfiguration.getFirstLevelModuleDependencies(
//              d -> d.getName().equals(v.getName())), v)
//      );
      resolvedConfiguration.rethrowFailure();
      getLogger().quiet("Successful changed vulnerable dependency versions to fixed");
    } else {
      //failure handling (jackson)
      List<String> unresolvedDependencyNames = unresolvedDependencyResults.stream().map(
          unresolvedDependencyResult -> {
            String displayName = unresolvedDependencyResult.getRequested().getDisplayName();
            //TODO don't do foreach loops for two lists, just use unresolvedDependencyResult.getFrom()?
            resolutionResult.getAllComponents().forEach(c -> c.getDependencies().stream()
                .filter(d -> d.getRequested().getDisplayName().equals(displayName))
                .forEach(d -> {
                  var identifier = c.getModuleVersion();
                  VulnerableDependency match = gavVulnerableDependencies.detect(
                      vd -> vd.isSameFixGAV(identifier.getGroup(), identifier.getName(),
                          identifier.getVersion()));
                  getLogger().debug("match: {}, dependencyResult: {}", match,
                      identifier);//TODO remove after debugging
                  //override version with other fix version
                  if (match != null) {
                    isRetryable.set(match.nextFixVersion());
                    getLogger().info("try next fix version {} {}", identifier,
                        match.getFixVersion());//TODO remove after debugging
                  }
                }));
            return displayName.substring(0, displayName.lastIndexOf(":"));
          }).distinct().toList();
      getLogger().error("Unresolved dependencies: {}", unresolvedDependencyNames);
      if (isRetryable.get()) {
        //retry override version
        getLogger().error("Couldn't resolve all dependencies. Retry version override");
        isRetryable.set(false);
        //Fixme Doesn't work: > Cannot change resolution strategy of dependency configuration ':runtimeClasspath' after it has been resolved.
        overrideDepencyVersionToFixed(vulnerableFixableDependencies, isRetryable);
      }

      throw new ResolveException(": Vulnerable dependency version override",
          Lists.immutable.ofAll(unresolvedDependencyResults)
              .collect(UnresolvedDependencyResult::getFailure));
    }
    return resolvedConfiguration;
  }
}
