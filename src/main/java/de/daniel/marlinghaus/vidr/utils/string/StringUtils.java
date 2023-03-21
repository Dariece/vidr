package de.daniel.marlinghaus.vidr.utils.string;

public final class StringUtils {

  public static boolean notBlank(String s) {
    return null != s && !"".equals(s);
  }
}
