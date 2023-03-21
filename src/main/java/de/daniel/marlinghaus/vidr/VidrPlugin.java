package de.daniel.marlinghaus.vidr;

import static de.daniel.marlinghaus.vidr.VidrTasks.CREATE_SBOM;
import static de.daniel.marlinghaus.vidr.VidrTasks.CREATE_VULNERABILITY_REPORT;
import static de.daniel.marlinghaus.vidr.task.vo.CvssSeverity.CRITICAL;
import static de.daniel.marlinghaus.vidr.task.vo.CvssSeverity.HIGH;

import de.daniel.marlinghaus.vidr.task.CreateVulnerabilityReport;
import de.daniel.marlinghaus.vidr.task.vo.ScanFormat;
import de.daniel.marlinghaus.vidr.task.vo.ScanJob;
import de.daniel.marlinghaus.vidr.vulnerability.VulnerabilityIdentifier;
import de.daniel.marlinghaus.vidr.vulnerability.VulnerabilityScanStrategyDeterminer;
import java.nio.file.Path;
import java.util.List;
import org.cyclonedx.gradle.CycloneDxTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public class VidrPlugin implements Plugin<Project> {

  /**
   * Configures the needed tasks and their configuration to apply to the target project
   *
   * @param targetProject The target project for the plugin
   */
  @Override
  public void apply(Project targetProject) {
    var tasks = targetProject.getTasks();
    String projectName = targetProject.getName();
    Path reportPath = targetProject.getBuildDir().toPath().resolve("reports");

    // register shared services like several incompatibilityChecker or vulnerabilityScanners
    Provider<VulnerabilityScanStrategyDeterminer> strategyDeterminer = targetProject.getGradle()
        .getSharedServices()
        .registerIfAbsent("vulnerabilityStrategyDeterminer",
            VulnerabilityScanStrategyDeterminer.class,
            spec -> spec.getParameters().getIdentifier()
                .set(spec.getParameters().getIdentifier().getOrElse(
                    VulnerabilityIdentifier.METADATA_DB_COMPARISION)));//TODO make configurable
//    registerBuildServices(targetProject);

    // configure sbom creation
    CycloneDxTask createSbomTask = tasks.register(CREATE_SBOM.getName(),
        CycloneDxTask.class,
        sbomTask -> {
          sbomTask.setGroup(VidrGroups.REPORTING.getName());
          sbomTask.setProjectType("application");
          sbomTask.setSchemaVersion("1.4");
          sbomTask.setDestination(reportPath.toFile());
          sbomTask.setOutputName(
              String.format("%s-%s-sbom", projectName, targetProject.getVersion()));
          sbomTask.setOutputFormat("json");
          sbomTask.setIncludeBomSerialNumber(false);
          sbomTask.setComponentVersion("2.0.0");
        }).get(); //TODO use output from previous cyclone Task in Project

    // configure vuln report creation
    CreateVulnerabilityReport createVulnerabilityReportTask = tasks.register(
        CREATE_VULNERABILITY_REPORT.getName(), CreateVulnerabilityReport.class,
        (vulnReportTask) -> {
          ScanJob scanJob = ScanJob.builder() // TODO make type nested in task
              .applicationName(projectName)
              .format(ScanFormat.SBOM)
              .stage(
                  "local") // TODO get from environment variables (SPRING_PROFILES_ACTIVE) or default local
              .pipelineRun("") // TODO get from environment variables or default empty
              .severities(List.of(HIGH, CRITICAL)) //TODO make configurable
              .build();

          vulnReportTask.setGroup(VidrGroups.REPORTING.getName());
          vulnReportTask.getStrategyDeterminer().set(strategyDeterminer);
          // define scan service default url
//          vulnReportTask.getServiceUrl().set(
//              vulnReportTask.getServiceUrl().getOrElse(URI.create("http://localhost:8100")));
          vulnReportTask.setScanObjectFile(
              createSbomTask.getDestination().get().toPath().resolve(createSbomTask.getOutputName()
                  .get() + ".json"));
          vulnReportTask.setScanJob(scanJob);

          // define execution order
          vulnReportTask.dependsOn(createSbomTask);
          vulnReportTask.mustRunAfter(createSbomTask);
        }
    ).get();
  }

  private void registerBuildServices(Project project) {
//    project.getGradle().getSharedServices().registerIfAbsent("trivyClient", TrivyClientService.class, spec -> spec.getParameters().getServiceUrl().set(
//        URI.create("http://localhost:8100")));

//    project.getObjects().namedDomainObjectSet(VulnerabilityScanService.class);

//    ExtensiblePolymorphicDomainObjectContainer<VulnerabilityScanService> vulnerabilityScanServices = project.getObjects()
//        .polymorphicDomainObjectContainer(VulnerabilityScanService.class);
//    vulnerabilityScanServices.registerBinding(VulnerabilityScanService.class, TrivyClientService.class);
//    vulnerabilityScanServices.registerBinding(VulnerabilityScanService.class, SteadyClientService.class);
//    vulnerabilityScanServices.register("trivyClient", TrivyClientService.class);
//    vulnerabilityScanServices.register("steadyClient", SteadyClientService.class);

  }
}
