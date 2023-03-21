package de.daniel.marlinghaus.vidr.utils.http;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.annotation.Contract;
import org.apache.hc.core5.annotation.ThreadingBehavior;
import org.apache.hc.core5.http.HttpEntity;


/**
 * Simple ResponseHandler to just return the responded IO-Stream
 */
@Contract(threading = ThreadingBehavior.UNSAFE)
public class OutputStreamHttpClientResponseHandler extends AbstractHttpClientResponseHandler<OutputStream>{

  /**
   * @param entity of response
   * @return IO stream from response
   * @throws IOException if stream is already closed
   */
  @Override
  public OutputStream handleEntity(final HttpEntity entity) throws IOException {
    OutputStream out = new ByteArrayOutputStream();
    entity.writeTo(out);
    return out;
  }
}
