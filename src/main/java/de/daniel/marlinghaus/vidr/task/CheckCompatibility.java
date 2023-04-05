package de.daniel.marlinghaus.vidr.task;

import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.VulnerableDependency;
import java.util.List;
import lombok.Setter;
import org.eclipse.collections.api.factory.Lists;

public abstract class CheckCompatibility extends IncompatibilityTask {

//  @Setter
//  private List<VulnerableDependency> directResolvableDependencies = Lists.mutable.empty();

  /**
   * Incompatibility check after vulnerability fix
   */
  @Override
  void execute() {

  }
}
