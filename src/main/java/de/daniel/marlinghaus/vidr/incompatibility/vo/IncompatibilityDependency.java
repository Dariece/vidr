package de.daniel.marlinghaus.vidr.incompatibility.vo;


import de.daniel.marlinghaus.vidr.vulnerability.resolve.vo.GavDependency;
import java.io.FileInputStream;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.gradle.api.artifacts.Configuration;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
//TODO extend from GAVDependency -> make abstract class
public class IncompatibilityDependency extends GavDependency {
  //container
  private Configuration configuration;

  //custom attributes
  @Setter
  private boolean fixed;
  private FileInputStream sourceCode;
  private FileInputStream byteCode;
  private List<IncompatibilityDependency> transitiveDependencies;
}
