package ru.maxow.mvpn.user;

import java.util.Comparator;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.ListUserDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionMapper;
import ru.maxow.mvpn.tariff.TariffMapper;

@Mapper(componentModel = "spring", uses = {SubscriptionMapper.class, TariffMapper.class})
public abstract class UserMapper {

  @Mapping(source = "user", target = "subscription", qualifiedByName = "userToSubscriptionDto")
  @Mapping(source = "role", target = "role")
  public abstract UserResponseDto toUserResponseDto(User user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "verificationCode", ignore = true)
  @Mapping(target = "subscriptions", ignore = true)
  @Mapping(target = "xuiId", ignore = true)
  @Mapping(target = "xuiSubscription", ignore = true)
  public abstract User toUser(CreateUserRequestDto userRequestDto);

  public ListUserDto toListUserDto(User user) {
    Optional<Subscription> subscriptionOptional = user.getSubscriptions().stream()
        .max(Comparator.comparing(Subscription::getStartDate));

    if (subscriptionOptional.isPresent()) {
      Subscription subscription = subscriptionOptional.get();
      String tariffName = subscription.getTariff() != null ? subscription.getTariff().getName() : null;

      return new ListUserDto()
          .id(user.getId())
          .fullName(user.getFullName())
          .role(user.getRole().name())
          .subscriptionStatus(subscription.getStatus())
          .endDate(subscription.getEndDate())
          .tariffName(tariffName);
    }
    return new ListUserDto()
        .id(user.getId())
        .fullName(user.getFullName())
        .role(user.getRole().name())
        .tariffName(null);
  }

  @Named("userToSubscriptionDto")
  protected SubscriptionResponseDto userToSubscriptionDto(User user) {
    if (user == null || user.getId() == null) {
      return null;
    }

    return user.getSubscriptions().stream()
        .max(Comparator.comparing(Subscription::getStartDate))
        .map(this::toSubscriptionResponseDto)
        .orElse(null);
  }

  @Mapping(target = "userId", source = "user.id")
  protected abstract SubscriptionResponseDto toSubscriptionResponseDto(Subscription subscription);
}
