package de.daniel.marlinghaus.vidr.task;

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
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.internal.artifacts.dependencies.DefaultImmutableVersionConstraint;
import org.gradle.api.internal.catalog.DependencyModel;
import org.gradle.api.tasks.TaskAction;

public abstract class ResolveDependencyFix extends DefaultTask {

  //TODO resolve services dynamically by strategy
  private final VulnerabilityReportDeserializer<TrivyVulnerability> reportDeserializer = new TrivyReportDeserializer();
  private final VulnerableDependencyFixResolver dependencyFixResolver = new VulnerableDependencyFixResolver(
      reportDeserializer);
  @Setter
  private Path reportFile;

  @TaskAction
  void run() {
    var configurationHandler = getProject().getConfigurations();
    try {
      //just testing: prototyping make generalizable
      //get vulnerable dependencies
      VulnerabilityReport deserializedReport = reportDeserializer.deserialize(
          reportFile);
      List<GavVulnerableDependency> vulnerableFixableDependencies = dependencyFixResolver.resolveFixes(
          deserializedReport.getVulnerabilities());
      if (!dependencyFixResolver.getUnfixableVulnerabilities().isEmpty()
          || !dependencyFixResolver.getUnfixableDependencies().isEmpty()) {
        getLogger().warn("Following Vulnerabilities are not fixable: {}",
            dependencyFixResolver.getUnfixableVulnerabilities());
        getLogger().warn("Following vulnerable Dependencies are not fixable: {}",
            dependencyFixResolver.getUnfixableDependencies());
      }

      //ändere die versionen der betroffenen dependencies auf die gefixten
      var dependencyHandler = getProject().getDependencies();
//      ExternalModuleDependencyFactory dependencyFactory = new DefaultExternalDependencyFactory();
//    dependencyHandler.add("ConfigurationName", DependencyNotationObject)
//    Dependency implementation = dependencyHandler.add("implementation", new GavVulnerableDependency());
      Configuration compileClasspath = configurationHandler.getByName("compileClasspath");
      var dependencySet = compileClasspath.getDependencies();

      //add changed dependencies
      //gemeinsame Version bei Frameworks z. B. gemeinsame Group org.springframework beachten
      vulnerableFixableDependencies.forEach(
          vulnerableDependency -> {
            DomainObjectSet<Dependency> matchingDependencies = dependencySet.matching(
                dependency -> dependency.getName().equals(vulnerableDependency.getArtifact())
                    && dependency.getGroup().equals(vulnerableDependency.getGroup())
                    && dependency.getVersion().equals(vulnerableDependency.getVersion()));
            if (!matchingDependencies.isEmpty()) {
              dependencySet.removeAll(matchingDependencies);
              dependencySet.add(dependencyHandler.add("implementation",
                  new DependencyModel(vulnerableDependency.getGroup(),
                      vulnerableDependency.getArtifact(), vulnerableDependency.getVersion(),
                      new DefaultImmutableVersionConstraint(vulnerableDependency.getVersion()),
                      this.getClass().getName())));//TODO generalize
            }
          });

      //resolves and downloads configuration (dependency changes)
      ResolvedConfiguration resolvedConfiguration = compileClasspath.getResolvedConfiguration(); //TODO analyze
//    configurationHandler.detachedConfiguration(implementation);

      //baue das projekt mit den geänderten Versionen oder erstelle neue sbom, je nach implementierung für prüfung
      //was wenn nicht baubar? Kann Plugin weiterlaufen?
    } catch (IOException e) {
      throw new GradleException("An error occurred executing " + this.getClass().getName(),
          e); //TODO failure handling
    }
  }
}
