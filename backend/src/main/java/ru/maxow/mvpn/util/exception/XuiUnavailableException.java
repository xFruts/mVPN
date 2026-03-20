package ru.maxow.mvpn.util.exception;

public class XuiUnavailableException extends RuntimeException {
  public XuiUnavailableException(String message) {
    super(message);
  }
  public XuiUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
