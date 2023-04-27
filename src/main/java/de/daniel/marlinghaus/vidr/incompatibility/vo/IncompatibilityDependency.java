package de.daniel.marlinghaus.vidr.incompatibility.vo;


import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.GavDependency;
import java.io.FileInputStream;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.gradle.api.artifacts.Configuration;
import sootup.java.core.JavaProject;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
@Data
public class IncompatibilityDependency extends GavDependency {

  //container
  private Configuration configuration;

  //custom attributes
  private boolean fixed;
  private FileInputStream sourceCode;
  //  private FileInputStream byteCode;
  private JavaProject byteCode;
  private JavaProject.JavaProjectBuilder byteCodeBuilder;
//  private JavaView byteCode;
  @Builder.Default
  private MutableList<IncompatibilityDependency> transitiveDependencies = Lists.mutable.empty();

  private boolean rootProject;
  private boolean transitiveProjectDependency;
  @Builder.Default
  private boolean loadedDependency = true;
  private int riskLevel;

  public IncompatibilityDependency copy(){
    return IncompatibilityDependency.builder()
        .byteCode(this.getByteCode())
        .group(this.getGroup())
        .name(this.getName())
        .version(this.getVersion())
        .rootProject(this.isRootProject())
        .transitiveDependencies(this.getTransitiveDependencies())
        .configuration(this.getConfiguration())
        .build();
  }
}
