package ru.maxow.mvpn.util.xss;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Custom JSON serializer that escapes HTML characters in strings to prevent XSS attacks.
 */
public class XssStringJsonSerializer extends JsonSerializer<String> {

  @Override
  public void serialize(
      String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    if (value != null) {
      String escapedValue = StringEscapeUtils.escapeHtml4(value);
      jsonGenerator.writeString(escapedValue);
    } else {
      jsonGenerator.writeNull();
    }
  }
}
