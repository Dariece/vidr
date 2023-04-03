package de.daniel.marlinghaus.vidr.utils.configuration;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

//TODO als shared service einbinden, damit Objekte wie objectMapper Singleton bleiben
public final class JsonConfiguration {

  private static ObjectMapper objectMapper;

  /**
   * @return json objectMapper singleton
   */
  public static ObjectMapper jsonMapper() {
    if (objectMapper == null) {
      objectMapper = new JsonMapper()
          .disable(
              SerializationFeature.FAIL_ON_EMPTY_BEANS,
              SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS
          ).enable(
              SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS,
              SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
          ).enable(Feature.IGNORE_UNDEFINED)
          .enable(
              DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
              DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
              DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE
          ).disable(
              DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
//            DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
//            DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS
          );
    }

    return objectMapper;
  }
}
