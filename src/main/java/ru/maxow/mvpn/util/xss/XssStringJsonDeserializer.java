package ru.maxow.mvpn.util.xss;


import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import java.io.IOException;

public class XssStringJsonDeserializer extends JsonDeserializer<String> {

  private static final PolicyFactory POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

  @Override
  public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
    String value = jsonParser.getValueAsString();
    if (value == null) {
      return null;
    }
    return POLICY.sanitize(value);
  }
}
