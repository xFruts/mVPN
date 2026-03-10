package ru.maxow.mvpn.payment.paymentverification;

import org.mapstruct.Mapper;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;

@Mapper(componentModel = "spring")
public interface PaymentVerificationMapper {


  PaymentVerificationResponseDto toDto(PaymentVerification paymentVerification);

  PaymentVerification toEntity(CreateUpdatePaymentVerificationDto dto);
}
