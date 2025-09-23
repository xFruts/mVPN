package ru.maxow.mvpn.payment;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentSettingsMapper {
  PaymentSettings toEntity(PaymentSettingsRequestDto dto);
  void updateEntityFromDto(PaymentSettingsRequestDto dto, @MappingTarget PaymentSettings entity);
}
