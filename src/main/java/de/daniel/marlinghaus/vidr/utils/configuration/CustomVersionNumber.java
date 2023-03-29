package de.daniel.marlinghaus.vidr.utils.configuration;


import javax.annotation.Nullable;
import org.gradle.util.internal.VersionNumber;

public class CustomVersionNumber extends VersionNumber {

  private static final String VERSION_TEMPLATE = "%d.%d.%d.%d%s";

  public CustomVersionNumber(int major, int minor, int micro, @Nullable String qualifier) {
    super(major, minor, micro, qualifier);
  }

  public CustomVersionNumber(int major, int minor, int micro, int patch,
      @Nullable String qualifier) {
    super(major, minor, micro, patch, qualifier);
  }

  @Override
  public String toString() {
    var retVal = super.toString();
    if (this.getPatch() == 0 && this.getMicro() == 0) {
      return retVal.substring(0, retVal.length() - 2);
    } else {
      return retVal;
    }
  }

  public static CustomVersionNumber parse(String versionString) {
    CustomVersionNumber customVersionNumber;
    VersionNumber versionNumber = VersionNumber.withPatchNumber().parse(versionString);

    //BUGFIX: Use different constructor, otherwise unknown versions like 2.13.4 != 2.13.4.0 are generated
    if (versionNumber.getPatch() == 0) {
      customVersionNumber = new CustomVersionNumber(versionNumber.getMajor(),
          versionNumber.getMinor(), versionNumber.getMicro(),
          versionNumber.getQualifier());
    } else {
      customVersionNumber = new CustomVersionNumber(versionNumber.getMajor(),
          versionNumber.getMinor(), versionNumber.getMicro(),
          versionNumber.getPatch(),
          versionNumber.getQualifier());
    }

    return customVersionNumber;
  }
}
