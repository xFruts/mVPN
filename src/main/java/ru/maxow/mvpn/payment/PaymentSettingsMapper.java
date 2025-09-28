package ru.maxow.mvpn.payment;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for converting between PaymentSettingsRequestDto and PaymentSettings entity.
 */
@Mapper(componentModel = "spring")
public interface PaymentSettingsMapper {

  /** Converts PaymentSettingsRequestDto to PaymentSettings entity. */
  @Mapping(source = "paymentDate", target = "paymentDate", dateFormat = "yyyy-MM-dd")
  PaymentSettingsResponseDto toResponseDto(PaymentSettings entity);

  /** Updates an existing PaymentSettings entity with values from PaymentSettingsRequestDto. */
  @Mapping(target = "id", ignore = true)
  void updateEntityFromDto(PaymentSettingsRequestDto dto, @MappingTarget PaymentSettings entity);
}
