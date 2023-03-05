package de.daniel.marlinghaus.vidr.incompatibility.pojo;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.gradle.api.artifacts.Configuration;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.util.List;

@Getter @Setter @SuperBuilder
public class Dependency
    implements org.gradle.api.artifacts.Dependency {

    //inherited attributes
    private String name;
    private String version;
    private String group;

    //container
    private Configuration configuration;

    //custom attributes
    private boolean fixed;
    private FileInputStream sourceCode;
    private FileInputStream byteCode;
    private List<Dependency> transitiveDependencies;

    //TODO implement
    @Override public boolean contentEquals(org.gradle.api.artifacts.Dependency dependency) {
        return false;
    }

    @Override public org.gradle.api.artifacts.Dependency copy() {
        return null;
    }

    @Nullable @Override public String getReason() {
        return null;
    }

    @Override public void because(@Nullable String reason) {

    }
}
