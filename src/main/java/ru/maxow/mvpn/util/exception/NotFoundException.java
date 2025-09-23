package ru.maxow.mvpn.util.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Exception thrown when an entity is not found in the database.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotFoundException extends RuntimeException {
  String entityName;
  Long identifier;

  /**
   * Constructs a new NotFoundException with the specified entity name and identifier.
   *
   * @param entityName the name of the entity that was not found
   * @param identifier the identifier of the entity that was not found
   */
  public NotFoundException(String entityName, Long identifier) {
    super(String.format("%s with identifier [%s] not found", entityName, identifier));
    this.entityName = entityName;
    this.identifier = identifier;
  }
}
