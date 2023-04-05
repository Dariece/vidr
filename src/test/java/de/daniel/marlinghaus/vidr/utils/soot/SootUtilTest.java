package de.daniel.marlinghaus.vidr.utils.soot;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.junit.jupiter.api.Test;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

class SootUtilTest {

  JavaProject thisProject;

  ///fixme
  @Test
  void testConfigureProjectPositive() {
    thisProject = SootUtil.configureProject(17, Path.of("build"), true);

    assertThat(thisProject).isNotNull();
    assertThat(thisProject).hasNoNullFieldsOrProperties();

    System.out.println("Java Projec vidr classes:");
    JavaView fullView = thisProject.createFullView();
    assertThat(fullView.getClassOrThrow(
        thisProject.getIdentifierFactory()
            .getClassType("classes.java.main." + SootUtil.class.getName())
    )).isNotNull();

    assertThat(fullView.getClasses().stream().map(JavaSootClass::getName).toList())
        .containsAll(SootUtil.getAllProjectClasses(thisProject).collect(JavaSootClass::getName));
  }

  @Test
  void testGetJarDependencyClasses() throws IOException {
    Path projectJar;
    try (var paths = Files.walk(Path.of("build", "libs"))) {
      projectJar = paths.filter(p -> !Files.isDirectory(p)).findFirst().orElseThrow();
    }

    ImmutableMap<String, ImmutableList<JavaSootClass>> jarDependencyClasses = SootUtil.getJarDependencyClasses(
        List.of(projectJar));

    assertThat(jarDependencyClasses).isNotNull();
    assertThat(jarDependencyClasses).isNotEmpty();

    System.out.println(
        jarDependencyClasses.collect(element -> element.collect(JavaSootClass::getName)));
  }
}
