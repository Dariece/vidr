package de.daniel.marlinghaus.vidr;

import de.daniel.marlinghaus.vidr.task.CreateVulnerabilityReport;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static de.daniel.marlinghaus.vidr.VidrTasks.CREATE_SBOM;

//@Component
//@SpringBootApplication
//@Slf4j
public class VIDRPlugin implements Plugin<Project> {
    //    public static void main(String[] args) {
    //        SpringApplication.run(VIDRPlugin.class, args);
    //    }

    @Override public void apply(Project target) {
        var tasks = target.getTasks();

        tasks.register(CREATE_SBOM.name(), CreateVulnerabilityReport.class);
        tasks.create("cyclonedxBom"); //TODO use output from previous cyclone Task in Project
    }
}
