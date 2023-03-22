package de.daniel.marlinghaus.vidr.utils.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommaSeparatedStringToListDeserializer extends StdScalarDeserializer<List<String>> {

  public CommaSeparatedStringToListDeserializer() {
    super(String.class);
  }

  @Override
  public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

    String str;
    List<String> retVal = new ArrayList<>();
    // [databind#381]
    if (p.hasToken(JsonToken.START_ARRAY)) {
      return _deserializeFromArray(p, ctxt);
    }

    // The critical path: ensure we handle the common case first.
    if (p.hasToken(JsonToken.VALUE_STRING)) {
      str = p.getText();
    } else {
      str = _parseString(p, ctxt, this);
    }

    Arrays.stream(str.split(",")).forEach(s -> retVal.add(s.trim()));

    return retVal;
  }
}
