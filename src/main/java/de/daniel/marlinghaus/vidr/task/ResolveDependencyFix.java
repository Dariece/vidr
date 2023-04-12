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
import org.gradle.api.artifacts.ArtifactView;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolveException;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.dsl.dependencies.DependencyFactoryInternal;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.component.local.model.OpaqueComponentIdentifier;

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

  //TODO move providers to one shared service/object for following tasks
  @Internal
  protected abstract Property<ResolvedConfiguration> getInternalResolvedConfiguration();

  /**
   * <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html">Lazy
   * configuration</a> to share object with following task
   */
  @Getter
  @Internal
  private Provider<ResolvedConfiguration> resolvedConfiguration = getInternalResolvedConfiguration()
      .map(t -> t);

  @Internal
  protected abstract Property<FileCollection> getInternalArtifactFilesBeforeFixup();

  /**
   * <a href="https://docs.gradle.org/current/userguide/lazy_configuration.html">Lazy
   * configuration</a> to share object with following task
   */
  @Getter
  @Internal
  private Provider<FileCollection> artifactFilesBeforeFixup = getInternalArtifactFilesBeforeFixup()
      .map(t -> t);

  @Getter
  @Internal
  private ResolvableDependencies resolvableDependencies;
  @Getter
  @Internal
  private List<VulnerableDependency> directResolvableDependencies = Lists.mutable.empty();
  private List<VulnerableDependency> unresolvableDependencies = Lists.mutable.empty();
  private Configuration runtimeClasspathOrigin;

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

      getInternalArtifactFilesBeforeFixup().set(getRuntimeClasspathBeforeFixup());
      getInternalResolvedConfiguration().set(overrideDepencyVersionToFixed(
          vulnerableFixableDependencies, new AtomicBoolean(false)));

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
    Configuration runtimeClasspath = getRuntimeClasspath();
    resolvableDependencies = runtimeClasspath.getIncoming(); //TODO try to get each configuration and apply override action on match
    var dependencySet = resolvableDependencies.getDependencies();
    getLogger().info("runtimeClasspath dependencies: {}",
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
                runtimeClasspath);
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
      resolvedConfiguration = runtimeClasspath.getResolvedConfiguration(); //TODO analyze
      resolvedConfiguration.rethrowFailure();
      getLogger().quiet("Successful changed vulnerable dependency versions to fixed");
    } else {
      //failure handling (for example jackson)
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

  //Get copy of configuration instance to make overrideDependency retryable
  private Configuration getRuntimeClasspath() {
    //TODO get before overrideDepencyVersionToFixed() and safe in field
    if (runtimeClasspathOrigin == null) {
      runtimeClasspathOrigin = getProject().getConfigurations().getByName(
          RUNTIME_CLASSPATH_CONFIGURATION_NAME);
    }

    return runtimeClasspathOrigin.copyRecursive();
  }

  //adapted by JavaPlugin.java BuildableJavaComponentImpl.getRuntimeClasspath()
  private FileCollection getRuntimeClasspathBeforeFixup() {
    Configuration confCopy = getRuntimeClasspath();
    getLogger().quiet("All dependencies: {}\n",
        confCopy.getIncoming().getResolutionResult().getAllDependencies());
    ArtifactView view = confCopy.getIncoming().artifactView(config -> {
      config.componentFilter(componentId -> {
        if (componentId instanceof OpaqueComponentIdentifier) {
          DependencyFactoryInternal.ClassPathNotation classPathNotation = ((OpaqueComponentIdentifier) componentId).getClassPathNotation();
          return classPathNotation != DependencyFactoryInternal.ClassPathNotation.GRADLE_API
              && classPathNotation != DependencyFactoryInternal.ClassPathNotation.LOCAL_GROOVY;
        }
        return true;
      });
    });
//    Configuration runtimeElements = getProject().getConfigurations()
//        .getByName(RUNTIME_ELEMENTS_CONFIGURATION_NAME);
//    return runtimeElements.getOutgoing().getArtifacts().getFiles().plus(view.getFiles());
    return view.getFiles();
  }
}
