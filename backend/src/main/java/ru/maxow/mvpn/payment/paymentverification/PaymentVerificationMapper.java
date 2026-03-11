package ru.maxow.mvpn.payment.paymentverification;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.user.User;

@Mapper(componentModel = "spring")
public interface PaymentVerificationMapper {

  PaymentVerificationResponseDto toDto(PaymentVerification paymentVerification);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "verifiedAt", ignore = true)
  @Mapping(target = "verifiedBy", ignore = true)
  @Mapping(target = "user", source = "userId")
  PaymentVerification toEntity(CreateUpdatePaymentVerificationDto dto);

  default User map(Long userId) {
    if (userId == null) {
      return null;
    }
    User user = new User();
    user.setId(userId);
    return user;
  }
}
