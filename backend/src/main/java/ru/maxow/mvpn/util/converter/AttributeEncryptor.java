package ru.maxow.mvpn.util.converter;

import jakarta.persistence.Converter;
import jakarta.persistence.AttributeConverter;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Converter
public final class AttributeEncryptor implements AttributeConverter<String, String> {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private final Key key;
  private final SecureRandom secureRandom;

  public AttributeEncryptor(@Value("${encryption.key}") String keyString) {
    byte[] keyBytes = keyString.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
      throw new IllegalArgumentException("Key must be 16, 24 or 32 bytes, got: " + keyBytes.length);
    }
    this.key = new SecretKeySpec(keyBytes, "AES");
    this.secureRandom = new SecureRandom();
  }


  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }

    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

      byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
      byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
      System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

      return Base64.getEncoder().encodeToString(encryptedWithIv);
    } catch (Exception e) {
      throw new IllegalStateException("Could not encrypt attribute", e);
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }

    try {
      byte[] decoded = Base64.getDecoder().decode(dbData);
      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(decoded, 0, iv, 0, iv.length);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

      byte[] encrypted = new byte[decoded.length - GCM_IV_LENGTH];
      System.arraycopy(decoded, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Could not decrypt dbData", e);
    }
  }
}
