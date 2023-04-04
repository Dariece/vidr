package de.daniel.marlinghaus.vidr.task;

import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.VulnerableDependency;
import java.util.List;
import lombok.Setter;
import org.eclipse.collections.api.factory.Lists;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.tasks.TaskAction;

public abstract class CheckCompatibility extends DefaultTask {

  @Setter
  private ResolvedConfiguration resolvedConfiguration;
  @Setter
  private List<VulnerableDependency> directResolvableDependencies = Lists.mutable.empty();

  @TaskAction
  void run() {

  }
}
