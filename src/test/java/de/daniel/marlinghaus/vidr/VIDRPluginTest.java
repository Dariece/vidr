package de.daniel.marlinghaus.vidr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class VIDRPluginTest {

  @TempDir
  File testProjectDir;
  private File settingsFile;
  private File buildFile;
  private final String projectName = "TestProject";

//  @BeforeEach
//  public void setup() {
//    settingsFile = new File(testProjectDir, "settings.gradle");
//    buildFile = new File(testProjectDir, "build.gradle");
//  }

  @Test
  void applyTest() {
    Project project = ProjectBuilder.builder().withName(projectName).build();

    var dependency = new DefaultExternalModuleDependency("TestGroup", "Test", "0.0.0",
        project.getConfigurations().create("Test").getName());
    project.getDependencies().add("Test", dependency);
    project.getTasks().register("build");
    project.getPluginManager().apply("de.daniel.marlinghaus.vidr");

    System.out.println(dependency.getTargetConfiguration());
    project.getTasks().forEach(t -> System.out.println("Task name: " + t.getName()));
    assertThat(project.getPluginManager().hasPlugin("de.daniel.marlinghaus.vidr")).isTrue();
  }

//  @Test
//  void functionalPluginTest() {
//    // Prepare Environment
//    var buildFileContent = """
////        plugins {
////          id 'org.cyclonedx.bom' version '1.7.1'
////        }
//
//        version = '1.0.0'
//
////        dependencies {
////          implementation ('commons-io:commons-io:2.6')
////        }
////
////        tasks.cyclonedxBom {
////        	setProjectType("application")
////        	setSchemaVersion("1.4")
////        	setDestination(project.file("build/reports"))
////        	setOutputName("sbom")
////        	setOutputFormat("json")
////        	setIncludeBomSerialNumber(false)
////        	setComponentVersion("2.0.0")
////        }
//        """;
//    writeFile(settingsFile, String.format("rootProject.name = '%s'", projectName));
//    writeFile(buildFile, buildFileContent);
//
//    // Run Task
//    String taskName = ":" + CREATE_SBOM.name();
//    BuildResult result = GradleRunner.create().withProjectDir(testProjectDir).withPluginClasspath()
//        .withArguments(taskName).build();
//
//    assertThat(Objects.requireNonNull(result.task(taskName))
//        .getOutcome()).isEqualByComparingTo(TaskOutcome.SUCCESS);
//  }

//  private void writeFile(File destination, String content) {
//    try (BufferedWriter output = new BufferedWriter(new FileWriter(destination))) {
//      output.write(content);
//    } catch (IOException e) {
//      throw new RuntimeException(String.format("Couldn't write testfile %s", destination.getName()),
//          e);
//    }
//  }
}
