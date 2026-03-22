package ru.maxow.mvpn.util.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AttributeEncryptor - unit tests")
class AttributeEncryptorTest {

  private static final String VALID_KEY = "0123456789ABCDEF0123456789ABCDEF";

  @Test
  @DisplayName("Encrypts and decrypts value with lossless round-trip")
  void shouldEncryptAndDecryptRoundTrip() {
    AttributeEncryptor encryptor = new AttributeEncryptor(VALID_KEY);
    String source = "sensitive-value";

    String encrypted = encryptor.convertToDatabaseColumn(source);
    String decrypted = encryptor.convertToEntityAttribute(encrypted);

    assertThat(encrypted).isNotBlank();
    assertThat(encrypted).isNotEqualTo(source);
    assertThat(decrypted).isEqualTo(source);
  }

  @Test
  @DisplayName("Returns null for null input values")
  void shouldHandleNullValues() {
    AttributeEncryptor encryptor = new AttributeEncryptor(VALID_KEY);

    assertThat(encryptor.convertToDatabaseColumn(null)).isNull();
    assertThat(encryptor.convertToEntityAttribute(null)).isNull();
  }

  @Test
  @DisplayName("Throws IllegalArgumentException for invalid AES key length")
  void shouldFailOnInvalidKeyLength() {
    assertThatThrownBy(() -> new AttributeEncryptor("12345678901234567"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Key must be 16, 24 or 32 bytes");
  }

  @Test
  @DisplayName("Throws IllegalStateException when encrypted payload is corrupted")
  void shouldFailOnTamperedCiphertext() {
    AttributeEncryptor encryptor = new AttributeEncryptor(VALID_KEY);
    String encrypted = encryptor.convertToDatabaseColumn("payload");

    byte[] decoded = Base64.getDecoder().decode(encrypted);
    decoded[decoded.length - 1] = (byte) (decoded[decoded.length - 1] ^ 0xFF);
    String tampered = Base64.getEncoder().encodeToString(decoded);

    assertThatThrownBy(() -> encryptor.convertToEntityAttribute(tampered))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Could not decrypt dbData");
  }
}

