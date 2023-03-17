package de.daniel.marlinghaus.vidr.utils.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copied from org.springframework.util to minimize transitive dependencies via spring. Original
 * content by:
 *
 * @author Juergen Hoeller, Phillip Webb, Brian Clozel
 */
public abstract class StreamUtils {

  /**
   * Copy the contents of the given InputStream to the given OutputStream.
   * <p>Leaves both streams open when done.
   *
   * @param in  the InputStream to copy from
   * @param out the OutputStream to copy to
   * @throws IOException in case of I/O errors
   */
  public static void copy(InputStream in, OutputStream out) throws IOException {
    if (in == null) {
      throw new RuntimeException("No InputStream specified");
    } else if (out == null) {
      throw new RuntimeException("No OutputStream specified");
    } else {
      in.transferTo(out);
      out.flush();
    }
  }
}
