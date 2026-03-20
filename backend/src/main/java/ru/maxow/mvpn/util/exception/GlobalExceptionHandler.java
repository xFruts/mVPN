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
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String CORRELATION_ID_KEY = "correlation-id";
  private static final String MSG_RESOURCE_NOT_FOUND = "Resource not found";
  private static final String MSG_INVALID_REQUEST = "Invalid request parameters";
  private static final String MSG_METHOD_NOT_ALLOWED = "Method not allowed";
  private static final String MSG_XUI_UNAVAILABLE = "XUI service is temporarily unavailable";
  private static final String MSG_UNEXPECTED = "An unexpected error occurred";

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public ErrorResponse handleNotFoundException(NotFoundException ex, WebRequest request) {
    log.info("Entity not found: entity={}, id={}, path={}",
        ex.getEntityName(),
        ex.getIdentifier(),
        extractPath(request));
    return new ErrorResponse(
        MSG_RESOURCE_NOT_FOUND,
        System.currentTimeMillis(),
        "NOT_FOUND",
        getOrGenerateCorrelationId()
    );
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NoResourceFoundException.class)
  public ErrorResponse handleNoResourceFoundException(NoResourceFoundException ex, WebRequest request) {
    log.info("No resource found: path={}", extractPath(request));
    return new ErrorResponse(
        MSG_RESOURCE_NOT_FOUND,
        System.currentTimeMillis(),
        "RESOURCE_NOT_FOUND",
        getOrGenerateCorrelationId()
    );
  }

  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ErrorResponse handleHttpRequestNotSupportedException(HttpRequestMethodNotSupportedException ex, WebRequest request) {
    log.warn("Method not allowed: method={}, path={}", ex.getMethod(), extractPath(request));
    return new ErrorResponse(
        MSG_METHOD_NOT_ALLOWED,
        System.currentTimeMillis(),
        "METHOD_NOT_ALLOWED",
        getOrGenerateCorrelationId()
    );
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler({
      BadRequestException.class,
      MultipartException.class,
      IllegalArgumentException.class
  })
  public ErrorResponse handleBadRequestException(Exception ex, WebRequest request) {
    log.warn("Bad request: type={}, path={}",
        ex.getClass().getSimpleName(),
        extractPath(request));
    String errorCode = determineErrorCode(ex);
    return new ErrorResponse(
        MSG_INVALID_REQUEST,
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
            error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
            (left, right) -> left.equals(right) ? left : left + "; " + right,
            LinkedHashMap::new
        ));
    log.warn("Validation failed: fields={}, path={}", fieldErrors.keySet(), extractPath(request));
    return new ValidationErrorResponse(
        "Validation failed",
        System.currentTimeMillis(),
        "VALIDATION_FAILED",
        getOrGenerateCorrelationId(),
        fieldErrors
    );
  }

  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  @ExceptionHandler(XuiUnavailableException.class)
  public ErrorResponse handleXuiUnavailableException(XuiUnavailableException ex, WebRequest request) {
    String correlationId = getOrGenerateCorrelationId();
    log.error("XUI unavailable [correlationId={}]: path={}", correlationId, extractPath(request), ex);
    return new ErrorResponse(
        MSG_XUI_UNAVAILABLE,
        System.currentTimeMillis(),
        "XUI_UNAVAILABLE",
        correlationId
    );
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public ErrorResponse handleGlobalException(Exception ex, WebRequest request) {
    String correlationId = getOrGenerateCorrelationId();
    log.error("Unexpected error [correlationId={}]: path={}", correlationId, extractPath(request), ex);
    return new ErrorResponse(
        MSG_UNEXPECTED,
        System.currentTimeMillis(),
        "INTERNAL_ERROR",
        correlationId
    );
  }

  private String determineErrorCode(Exception ex) {
    if (ex instanceof BadRequestException) return "BAD_REQUEST";
    if (ex instanceof MultipartException) return "INVALID_FILE_UPLOAD";
    if (ex instanceof IllegalArgumentException) return "BAD_REQUEST";
    return "BAD_REQUEST";
  }

  private String getOrGenerateCorrelationId() {
    String correlationId = MDC.get(CORRELATION_ID_KEY);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = java.util.UUID.randomUUID().toString();
      MDC.put(CORRELATION_ID_KEY, correlationId);
    }
    return correlationId;
  }

  private String extractPath(WebRequest request) {
    String description = request.getDescription(false); // uri=/path?query
    if (description == null) return "unknown";
    String path = description.startsWith("uri=") ? description.substring(4) : description;
    int queryIndex = path.indexOf('?');
    return queryIndex >= 0 ? path.substring(0, queryIndex) : path;
  }
}
