package de.daniel.marlinghaus.vidr.utils.soot;


import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
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

    assertThat(SootUtil.getAllProjectClasses(thisProject).stream().map(JavaSootClass::getName)
        .toList()).containsAll(fullView.getClasses().stream().map(JavaSootClass::getName).toList());
  }
}
