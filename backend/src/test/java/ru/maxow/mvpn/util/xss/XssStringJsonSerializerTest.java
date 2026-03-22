package ru.maxow.mvpn.util.xss;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("XssStringJsonSerializer - unit tests")
class XssStringJsonSerializerTest {

  private final XssStringJsonSerializer serializer = new XssStringJsonSerializer();

  @Test
  @DisplayName("Escapes HTML and script content")
  void shouldEscapeHtml() throws Exception {
    JsonGenerator jsonGenerator = mock(JsonGenerator.class);
    SerializerProvider serializerProvider = mock(SerializerProvider.class);

    serializer.serialize("<script>alert('xss')</script>", jsonGenerator, serializerProvider);

    verify(jsonGenerator).writeString("&lt;script&gt;alert('xss')&lt;/script&gt;");
  }

  @Test
  @DisplayName("Keeps plain text unchanged")
  void shouldKeepPlainText() throws Exception {
    JsonGenerator jsonGenerator = mock(JsonGenerator.class);
    SerializerProvider serializerProvider = mock(SerializerProvider.class);

    serializer.serialize("plain-text_123", jsonGenerator, serializerProvider);

    verify(jsonGenerator).writeString("plain-text_123");
  }

  @Test
  @DisplayName("Writes null when input value is null")
  void shouldWriteNullForNullValue() throws Exception {
    JsonGenerator jsonGenerator = mock(JsonGenerator.class);
    SerializerProvider serializerProvider = mock(SerializerProvider.class);

    serializer.serialize(null, jsonGenerator, serializerProvider);

    verify(jsonGenerator).writeNull();
  }
}

