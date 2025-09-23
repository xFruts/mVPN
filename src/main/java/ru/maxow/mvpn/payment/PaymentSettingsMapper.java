package ru.maxow.mvpn.payment;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * Mapper for converting between PaymentSettingsRequestDto and PaymentSettings entity.
 */
@Mapper(componentModel = "spring")
public interface PaymentSettingsMapper {
  /** Converts PaymentSettingsRequestDto to PaymentSettings entity. */
  PaymentSettings toEntity(PaymentSettingsRequestDto dto);

  /** Updates an existing PaymentSettings entity with values from PaymentSettingsRequestDto. */
  void updateEntityFromDto(PaymentSettingsRequestDto dto, @MappingTarget PaymentSettings entity);
}
