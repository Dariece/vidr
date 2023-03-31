package de.daniel.marlinghaus.vidr.incompatibility.vo;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.gradle.api.artifacts.Configuration;

import java.io.FileInputStream;
import java.util.List;

@Getter
@Builder
//TODO extend from GAVDependency -> make abstract class
public class IncompatibilityDependency {

  //inherited attributes
  private String name;
  private String version;
  private String group;

  //container
  private Configuration configuration;

  //custom attributes
  @Setter
  private boolean fixed;
  private FileInputStream sourceCode;
  private FileInputStream byteCode;
  private List<IncompatibilityDependency> transitiveDependencies;
}
