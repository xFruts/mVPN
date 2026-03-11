package ru.maxow.mvpn.payment.paymentsettings;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.maxow.mvpn.model.CreateUpdatePaymentSettingsDto;
import ru.maxow.mvpn.model.PaymentSettingsResponseDto;

import java.time.Instant;
import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface PaymentSettingsMapper {
  PaymentSettingsResponseDto toDto(PaymentSettings paymentSettings);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  PaymentSettings toEntity(CreateUpdatePaymentSettingsDto dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  void updateEntityFromDto(CreateUpdatePaymentSettingsDto dto, @MappingTarget PaymentSettings entity);

  default OffsetDateTime toOffsetDateTime(Instant instant) {
    return instant == null ? null : OffsetDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
  }
}
