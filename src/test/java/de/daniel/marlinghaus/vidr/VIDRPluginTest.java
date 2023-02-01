package de.daniel.marlinghaus.vidr;

import de.daniel.marlinghaus.vidr.incompatibility.pojo.Dependency;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VIDRPluginTest {
    @Test void applyTest() {
        Project project = ProjectBuilder.builder().withName("TestProject").build();
        var dependency = Dependency.builder().name("Test").version("0.0.0")
            .configuration(project.getConfigurations().create("Test")).version("0.0.1").build();
        project.getDependencies().add("Test", dependency);
        project.getPluginManager().apply("de.daniel.marlinghaus.vidr");

        System.out.println(dependency.getConfiguration());

        project.getTasks().forEach(t -> System.out.println("Task name: " + t.getName()));
        assertThat(project.getPluginManager().hasPlugin("de.daniel.marlinghaus.vidr")).isTrue();
    }
}
