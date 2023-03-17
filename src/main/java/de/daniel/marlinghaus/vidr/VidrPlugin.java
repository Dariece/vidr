package de.daniel.marlinghaus.vidr;

import static de.daniel.marlinghaus.vidr.VidrTasks.CREATE_SBOM;
import static de.daniel.marlinghaus.vidr.VidrTasks.CREATE_VULNERABILITY_REPORT;

import de.daniel.marlinghaus.vidr.task.CreateVulnerabilityReport;
import de.daniel.marlinghaus.vidr.task.vo.ScanFormat;
import de.daniel.marlinghaus.vidr.task.vo.ScanJob;
import org.cyclonedx.gradle.CycloneDxTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

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

    // configure sbom creation
    CycloneDxTask createSbomTask = tasks.register(CREATE_SBOM.getName(),
        CycloneDxTask.class,
        sbomTask -> {
          sbomTask.setGroup(VidrGroups.REPORTING.getName());
          sbomTask.setProjectType("application");
          sbomTask.setSchemaVersion("1.4");
          sbomTask.setDestination(targetProject.getBuildDir().toPath().resolve("reports").toFile());
          sbomTask.setOutputName(
              String.format("%s-%s-sbom", projectName, targetProject.getVersion()));
          sbomTask.setOutputFormat("json");
          sbomTask.setIncludeBomSerialNumber(false);
          sbomTask.setComponentVersion("2.0.0");
        }).get(); //TODO use output from previous cyclone Task in Project

    // configure vuln report creation
    CreateVulnerabilityReport createVulnerabilityReportTask = tasks.register(CREATE_VULNERABILITY_REPORT.getName(), CreateVulnerabilityReport.class,
        (vulnReportTask) -> {
          vulnReportTask.setGroup(VidrGroups.REPORTING.getName());
          // define scan service default url
//          vulnReportTask.getServiceUrl().set(
//              vulnReportTask.getServiceUrl().getOrElse(URI.create("http://localhost:8100")));
          vulnReportTask.setScanObjectFile(
              createSbomTask.getDestination().get().toPath().resolve(createSbomTask.getOutputName()
                  .get()));
          vulnReportTask.setScanJob(
              ScanJob.builder()
                  .applicationName(projectName)
                  .format(ScanFormat.SBOM)
                  .stage("local") //TODO get from environment variables (SPRING_PROFILES_ACTIVE) or default local
                  .pipelineRun("") //TODO get from environment variables or default empty
                  .build()//TODO use enum
          );

          // define execution order
          vulnReportTask.dependsOn(createSbomTask);
          vulnReportTask.mustRunAfter(createSbomTask);
        }
    ).get();
  }
}
