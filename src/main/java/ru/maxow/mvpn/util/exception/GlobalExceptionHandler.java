package ru.maxow.mvpn.util.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public ErrorResponse handleNotFoundException(NotFoundException ex, WebRequest request) {
    log.info("Entity Not Found: {}, Request details {}", ex, request);
    String userFriendlyMessage = "Resource not found";
    return new ErrorResponse(userFriendlyMessage, System.currentTimeMillis());
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(BadRequestException.class)
  public ErrorResponse handleBadRequestException(BadRequestException ex, WebRequest request) {
    log.info("Bad Request: {}, Request details {}", ex, request);
    String userFriendlyMessage = "Bad Request";
    return new ErrorResponse(userFriendlyMessage, System.currentTimeMillis());
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public ErrorResponse handleGlobalException(Exception ex, WebRequest request) {
    log.error("""
        Unexpected error.
        Message: {},
        Error: {},
        Request: {}
        """, ex.getMessage(), ex, request);
    return new ErrorResponse("An unexpected error occurred", System.currentTimeMillis());
  }
}
