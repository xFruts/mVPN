package ru.maxow.mvpn.util.exception;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public ErrorResponse handleNotFoundException(NotFoundException ex, WebRequest request) {
    log.info("Entity not found: entity={}, id={}, path={}",
        ex.getEntityName(),
        ex.getIdentifier(),
        request.getDescription(false));
    return new ErrorResponse(
        "Resource not found",
        System.currentTimeMillis(),
        "NOT_FOUND",
        getOrGenerateCorrelationId()
    );
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
      BadRequestException.class,
      MultipartException.class,
      HttpRequestMethodNotSupportedException.class
  })
  public ErrorResponse handleBadRequestException(Exception ex, WebRequest request) {
    log.warn("Bad Request: type={}, path={}",
        ex.getClass().getSimpleName(),
        request.getDescription(false));
    String errorCode = determineErrorCode(ex);
    return new ErrorResponse(
        "Invalid request parameters",
        System.currentTimeMillis(),
        errorCode,
        getOrGenerateCorrelationId());
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ErrorResponse handleDataIntegrityViolationException(
      DataIntegrityViolationException ex, WebRequest request) {
    log.warn("Data Integrity Violation: path={}", request.getDescription(false));
    log.debug("Full error: ", ex);
    return new ErrorResponse(
        "Data conflict occurred",
        System.currentTimeMillis(),
        "DATA_CONFLICT",
        getOrGenerateCorrelationId()
    );
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ValidationErrorResponse handleValidation(
      MethodArgumentNotValidException ex, WebRequest request) {
    Map<String, String> fieldErrors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
        ));
    log.warn("Validation failed: fields={}", fieldErrors.keySet());
    return new ValidationErrorResponse(
        "Validation failed",
        System.currentTimeMillis(),
        "VALIDATION_FAILED",
        getOrGenerateCorrelationId(),
        fieldErrors
    );
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public ErrorResponse handleGlobalException(Exception ex, WebRequest request) {
    String correlationId = getOrGenerateCorrelationId();

    log.error("Unexpected error [correlationId={}]: path={}",
        correlationId,
        request.getDescription(false));
    log.error("Exception details: ", ex);
    return new ErrorResponse(
        "An unexpected error occurred",
        System.currentTimeMillis(),
        "INTERNAL_ERROR",
        correlationId
    );
  }

  private String determineErrorCode(Exception ex) {
    if (ex instanceof BadRequestException) return "BAD_REQUEST";
    if (ex instanceof MultipartException) return "INVALID_FILE_UPLOAD";
    if (ex instanceof HttpRequestMethodNotSupportedException) return "METHOD_NOT_ALLOWED";
    return "BAD_REQUEST";
  }

  private String getOrGenerateCorrelationId() {
    String correlationId = MDC.get("correlation-id");
    if (correlationId == null) {
      correlationId = java.util.UUID.randomUUID().toString();
      MDC.put("correlation-id", correlationId);
    }
    return correlationId;
  }
}
