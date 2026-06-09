package ru.maxow.mvpn.util.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class XuiUnavailableException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  public XuiUnavailableException(String message) {
    super(message);
  }

  public XuiUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
