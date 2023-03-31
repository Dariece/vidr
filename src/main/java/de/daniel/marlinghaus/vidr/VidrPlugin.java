package de.daniel.marlinghaus.vidr;

import static de.daniel.marlinghaus.vidr.VidrTasks.CREATE_SBOM;
import static de.daniel.marlinghaus.vidr.VidrTasks.CREATE_VULNERABILITY_REPORT;
import static de.daniel.marlinghaus.vidr.VidrTasks.RESOLVE_DEPENDENCY_FIX;
import static de.daniel.marlinghaus.vidr.vulnerability.report.vo.CvssSeverity.CRITICAL;
import static de.daniel.marlinghaus.vidr.vulnerability.report.vo.CvssSeverity.HIGH;

import de.daniel.marlinghaus.vidr.task.CreateVulnerabilityReport;
import de.daniel.marlinghaus.vidr.task.ResolveDependencyFix;
import de.daniel.marlinghaus.vidr.utils.string.StringUtils;
import de.daniel.marlinghaus.vidr.vulnerability.VulnerabilityIdentifier;
import de.daniel.marlinghaus.vidr.vulnerability.VulnerabilityScanStrategyDeterminer;
import de.daniel.marlinghaus.vidr.vulnerability.scanner.vo.ScanFormat;
import de.daniel.marlinghaus.vidr.vulnerability.scanner.vo.ScanJob;
import java.nio.file.Path;
import java.util.List;
import org.cyclonedx.gradle.CycloneDxTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

public class VidrPlugin implements Plugin<Project> {


  private ProviderFactory providerFactory;

  /**
   * Configures the needed tasks and their configuration to apply to the target project
   *
   * @param targetProject The target project for the plugin
   */
  @Override
  public void apply(Project targetProject) {
    var tasks = targetProject.getTasks();
    providerFactory = targetProject.getProviders();
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
        vulnReportTask -> {

          String stage = getPropertyValue(null, null, "SPRING_PROFILES_ACTIVE");
          ScanJob scanJob = ScanJob.builder() // TODO make type nested in task
              .applicationName(projectName)
              .format(ScanFormat.SBOM)
              .stage(StringUtils.notBlank(stage) ? stage : "local")
              .pipelineRun("") // TODO get from environment variables or default empty
              .severities(List.of(HIGH, CRITICAL)) //TODO make configurable
              .build();
          Path scanObjectFile = createSbomTask.getDestination().get().toPath()
              .resolve(createSbomTask.getOutputName()
                  .get() + ".json");

          vulnReportTask.setGroup(VidrGroups.REPORTING.getName());
          vulnReportTask.getStrategyDeterminer().set(strategyDeterminer);
          // define scan service default url
//          vulnReportTask.getServiceUrl().set(
//              vulnReportTask.getServiceUrl().getOrElse(URI.create("http://localhost:8100")));
          vulnReportTask.setScanObjectFile(
              scanObjectFile);
          vulnReportTask.setScanJob(scanJob);
          vulnReportTask.setOutputFile(scanObjectFile.getParent().resolve(
              String.format("%s-%s-%s-trivy-report.json",
                  scanJob.getApplicationName(),
                  scanJob.getStage(),
                  scanJob.getPipelineRun())));// define output filename depending on scanJob

          // define execution order
          vulnReportTask.dependsOn(createSbomTask);
          vulnReportTask.mustRunAfter(createSbomTask);
        }).get();

    //configure dependency fix resolve
    ResolveDependencyFix resolveDependencyFix = tasks.register(RESOLVE_DEPENDENCY_FIX.getName(),
        ResolveDependencyFix.class,
        resolveDependencyFixTask -> {
          //TODO configure
          resolveDependencyFixTask.setReportFile(createVulnerabilityReportTask.getOutputFile());

          // define execution order
          resolveDependencyFixTask.dependsOn(createVulnerabilityReportTask);
          resolveDependencyFixTask.mustRunAfter(createVulnerabilityReportTask);
          //build after to safe result
          resolveDependencyFixTask.finalizedBy(tasks.named(JavaBasePlugin.BUILD_TASK_NAME).get());
        }).get();

    //TODO vorhandene Duplikate an Dependencies und falschen transitiven Abhängigkeitsversionen vermeiden
    CycloneDxTask createFixedSbomTask = tasks.register(CREATE_SBOM.getName() + "Fixed",
        CycloneDxTask.class,
        sbomTask -> {
          sbomTask.setGroup(VidrGroups.REPORTING.getName());
          sbomTask.setProjectType("application");
          sbomTask.setSchemaVersion("1.4");
          sbomTask.setDestination(reportPath.toFile());
          sbomTask.setOutputName(
              String.format("%s-%s-sbom-fixed", projectName, targetProject.getVersion()));
          sbomTask.setOutputFormat("json");
          sbomTask.setIncludeBomSerialNumber(false);
          sbomTask.setComponentVersion("2.0.0");

          // define execution order
          sbomTask.dependsOn(resolveDependencyFix);
          sbomTask.mustRunAfter(resolveDependencyFix);
        }).get();
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

  //TODO write Util Buildservice for this
  private String getPropertyValue(String systemPropertyName, String gradlePropertyName,
      String environmentName) {
    String retVal = "";

    //for details refer to gradle doc: https://docs.gradle.org/current/userguide/build_environment.html
    if (StringUtils.notBlank(systemPropertyName)) {
      retVal = providerFactory.systemProperty(systemPropertyName).getOrElse(retVal);
    } else if (StringUtils.notBlank(gradlePropertyName)) {
      retVal = providerFactory.gradleProperty(gradlePropertyName).getOrElse(retVal);
    } else {
      retVal = providerFactory.environmentVariable(environmentName).getOrElse(retVal);
    }

    return retVal;
  }
}
