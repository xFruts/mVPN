package ru.maxow.mvpn.payment.paymentverification;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface PaymentVerificationMapper {

  @Mapping(target = "userId", source = "user.id")
  PaymentVerificationResponseDto toDto(PaymentVerification paymentVerification);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "verifiedAt", ignore = true)
  @Mapping(target = "verifiedBy", ignore = true)
  @Mapping(target = "user", ignore = true)
  PaymentVerification toEntity(CreateUpdatePaymentVerificationDto dto);


  default OffsetDateTime map(Instant value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC);
  }

  default Instant map(OffsetDateTime value) {
    return value == null ? null : value.toInstant();
  }
}
