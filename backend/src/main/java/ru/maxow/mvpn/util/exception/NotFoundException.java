package ru.maxow.mvpn.util.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import java.io.Serial;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotFoundException extends RuntimeException {
  String entityName;
  String identifier;

  @Serial
  private static final long serialVersionUID = 1L;

  public NotFoundException(String message) {
    super(message);
    this.entityName = null;
    this.identifier = null;
  }

  public NotFoundException(Class<?> entityClass, Object identifier) {
    super(String.format("%s with identifier [%s] not found", entityClass.getSimpleName(), identifier));
    this.entityName = entityClass.getSimpleName();
    this.identifier = String.valueOf(identifier);
  }

  public NotFoundException(String entityName, Object identifier) {
    super(String.format("%s with identifier [%s] not found", entityName, identifier));
    this.entityName = entityName;
    this.identifier = String.valueOf(identifier);
  }
}
