package de.daniel.marlinghaus.vidr.utils.soot;

import java.nio.file.Path;
import java.util.Collection;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.inputlocation.PathBasedAnalysisInputLocation;
import sootup.java.core.JavaProject;
import sootup.java.core.JavaSootClass;
import sootup.java.core.language.JavaLanguage;

public class SootUtil {

//  public static List<String> getJarClasses(String path) {
//    if (new File(path).exists()) {
//      if (!path.endsWith("tar.gz") && !path.endsWith(".pom") && !path.endsWith(".war")) {
//        return SourceLocator.v().getClassesUnder(path);
//      } else {
//        MavenUtil.i().getLog().warn(path + "is illegal classpath");
//      }
//    } else {
//      MavenUtil.i().getLog().warn(path + "doesn't exist in local");
//    }
//    return new ArrayList<String>();
//  }

  public static JavaProject configureProject(int javaVersion, Path binaryPath,
      boolean isRootProject) {
    JavaLanguage language = new JavaLanguage(javaVersion);
    AnalysisInputLocation<JavaSootClass> inputLocation =
        new PathBasedAnalysisInputLocation(binaryPath,
            isRootProject ? SourceType.Application : SourceType.Library);

    return JavaProject.builder(language).addInputLocation(inputLocation).build();
  }

  public static Collection<JavaSootClass> getAllProjectClasses(JavaProject javaProject) {
    return javaProject.createOnDemandView().getClasses();
  }
}
