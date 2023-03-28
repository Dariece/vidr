package de.daniel.marlinghaus.vidr.task;

import de.daniel.marlinghaus.vidr.task.action.OverrideDependencyVersion;
import de.daniel.marlinghaus.vidr.vulnerability.report.TrivyReportDeserializer;
import de.daniel.marlinghaus.vidr.vulnerability.report.VulnerabilityReportDeserializer;
import de.daniel.marlinghaus.vidr.vulnerability.report.vo.VulnerabilityReport;
import de.daniel.marlinghaus.vidr.vulnerability.report.vo.trivy.TrivyVulnerability;
import de.daniel.marlinghaus.vidr.vulnerability.resolve.VulnerableDependencyFixResolver;
import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.GavVulnerableDependency;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.Setter;
import org.gradle.api.DefaultTask;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.tasks.TaskAction;

public abstract class ResolveDependencyFix extends DefaultTask {

  //TODO resolve services dynamically by strategy
  private final VulnerabilityReportDeserializer<TrivyVulnerability> reportDeserializer = new TrivyReportDeserializer();
  private final VulnerableDependencyFixResolver<TrivyReportDeserializer> dependencyFixResolver = new VulnerableDependencyFixResolver(
      reportDeserializer);
  @Setter
  private Path reportFile;

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
      List<GavVulnerableDependency> vulnerableFixableDependencies = dependencyFixResolver.resolveFixes(
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

      //ändere die versionen der betroffenen dependencies auf die gefixten
      var dependencyHandler = getProject().getDependencies();
      var configurationContainer = getProject().getConfigurations();
//      ExternalModuleDependencyFactory dependencyFactory = new DefaultExternalDependencyFactory();
      Configuration implementationConfiguration = configurationContainer.getByName(
          "runtimeClasspath");
      ResolvableDependencies resolvableDependencies = implementationConfiguration.getIncoming(); //TODO try to get each configuration and apply override action on match
//      var dependencySet = implementationConfiguration.getDependencies();
      var dependencySet = resolvableDependencies.getDependencies();
      getLogger().info("implementationConfiguration dependencies: {}",
          dependencySet.stream().toList());

      getLogger().info("Change vulnerable dependency versions to fixed");
      //add changed dependencies
      //gemeinsame Version bei Frameworks z. B. gemeinsame Group org.springframework beachten
      vulnerableFixableDependencies.forEach(
          vulnerableDependency -> {
            DomainObjectSet<Dependency> matchingDependencies = dependencySet.matching(
                dependency -> vulnerableDependency.getName().equals(dependency.getName())
                    && vulnerableDependency.getGroup().equals(dependency.getGroup()));
            if (!matchingDependencies.isEmpty()) {
              getLogger().info("Match consisting of: {}", matchingDependencies.stream().toList());
              new OverrideDependencyVersion(vulnerableDependency).execute(
                  implementationConfiguration);
            } else {
              getLogger().error("No match for: {}",
                  vulnerableDependency.getDependencyName());//TODO use other configuration if not found
            }
          });

      //resolves and downloads configuration (dependency changes)
      ResolutionResult resolutionResult = resolvableDependencies.getResolutionResult();//TODO analyze
      getLogger().info("Resolution result: {}", resolutionResult.getAllDependencies());
      ResolvedConfiguration resolvedConfiguration = implementationConfiguration.getResolvedConfiguration(); //TODO analyze
      getLogger().quiet("Successful changed vulnerable dependency versions to fixed");
      vulnerableFixableDependencies.forEach(
          v -> getLogger().quiet("{} {}", resolvedConfiguration.getFirstLevelModuleDependencies(
              d -> d.getName().equals(v.getName())), v)
      );
//      ResolvedConfiguration resolvedConfiguration = configurationContainer.detachedConfiguration(
//          dependencySet.toArray(new Dependency[0])).getResolvedConfiguration(); //TODO analyze
//      getLogger().quiet("{}, {}", dependencySet.toArray(new Dependency[0]),resolvedConfiguration.getFirstLevelModuleDependencies());

      //baue das projekt mit den geänderten Versionen oder erstelle neue sbom, je nach implementierung für prüfung
      //was wenn nicht baubar? Kann Plugin weiterlaufen?
    } catch (IOException e) {
      getLogger().error(" {} {}", e.getCause(), e.getMessage());
      throw new GradleException("An error occurred executing " + this.getClass().getName(),
          e); //TODO failure handling
    }
  }
}
