package de.daniel.marlinghaus.vidr;

import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.springframework.stereotype.Component;

@Component @Slf4j public class VIDRPlugin implements Plugin<Project> {
    @Override public void apply(Project target) {
        var dependencies = target.getDependencies();
        var configs = target.getConfigurations();

        log.info("Dependencies: {}", dependencies);
        log.info("Configs: {}", configs);

        target.task("This is a test").doLast(task -> {
            System.out.println(dependencies);
            System.out.println(configs);
            System.out.println("This is a test");
        });
        System.out.println("Config Names: " + configs.getNames());
        configs.forEach(c -> System.out.println("Version of first Dependency: " +  c.getDependencies().stream().findFirst().orElseThrow().getVersion()));
    }
}
