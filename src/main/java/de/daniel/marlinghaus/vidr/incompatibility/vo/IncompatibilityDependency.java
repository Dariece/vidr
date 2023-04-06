package de.daniel.marlinghaus.vidr.incompatibility.vo;


import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.GavDependency;
import java.io.FileInputStream;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.gradle.api.artifacts.Configuration;
import sootup.java.core.JavaProject;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class IncompatibilityDependency extends GavDependency {
  //container
  private Configuration configuration;

  //custom attributes
  @Setter
  @Builder.Default
  private boolean fixed = false;
  private FileInputStream sourceCode;
//  private FileInputStream byteCode;
  private JavaProject byteCode;
  @Builder.Default
  private MutableList<IncompatibilityDependency> transitiveDependencies = Lists.mutable.empty();
}
