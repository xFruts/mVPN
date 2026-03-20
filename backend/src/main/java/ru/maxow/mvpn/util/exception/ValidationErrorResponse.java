package ru.maxow.mvpn.util.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationErrorResponse extends ErrorResponse {
  Map<String, String> fieldErrors = new LinkedHashMap<>();

  public ValidationErrorResponse(String message, Long timestamp, String errorCode,
                                 String correlationId, Map<String, String> fieldErrors) {
    super(message, timestamp, errorCode, correlationId);
    this.fieldErrors = copy(fieldErrors);
  }

  public Map<String, String> getFieldErrors() {
    return Collections.unmodifiableMap(fieldErrors);
  }

  public void setFieldErrors(Map<String, String> fieldErrors) {
    this.fieldErrors = copy(fieldErrors);
  }

  private Map<String, String> copy(Map<String, String> source) {
    return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
  }
}
