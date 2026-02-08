package ru.maxow.mvpn.util.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationErrorResponse extends ErrorResponse {
  Map<String, String> fieldErrors;

  public ValidationErrorResponse(String message, Long timestamp, String errorCode,
                                 String correlationId, Map<String, String> fieldErrors) {
    super(message, timestamp, errorCode, correlationId);
    this.fieldErrors = fieldErrors;
  }
}
