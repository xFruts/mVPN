package ru.maxow.mvpn.payment.paymentsettings;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.maxow.mvpn.payment.paymentsettings.dto.CreateUpdatePaymentSettingsDto;
import ru.maxow.mvpn.payment.paymentsettings.dto.PaymentSettingsResponseDto;

@Mapper(componentModel = "spring")
public interface PaymentSettingsMapper {
  PaymentSettingsResponseDto toDto(PaymentSettings paymentSettings);

  PaymentSettings toEntity(CreateUpdatePaymentSettingsDto dto);

  @Mapping(target = "id", ignore = true)
  void updateEntityFromDto(CreateUpdatePaymentSettingsDto dto, @MappingTarget PaymentSettings entity);
}
