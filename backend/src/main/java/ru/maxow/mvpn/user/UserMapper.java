package ru.maxow.mvpn.user;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.ListUserDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionMapper;

@Mapper(componentModel = "spring")
@RequiredArgsConstructor
public abstract class UserMapper {

  SubscriptionMapper subscriptionMapper;

  @Mapping(source = "user", target = "subscription", qualifiedByName = "userToSubscriptionDto")
  @Mapping(source = "role", target = "role")
  public abstract UserResponseDto toUserResponseDto(User user);

  public abstract User toUser(CreateUserRequestDto userRequestDto);

  public ListUserDto toListUserDto(User user) {
    Optional<Subscription> subscriptionOptional = user.getSubscriptions().stream()
        .max(Comparator.comparing(Subscription::getStartDate));

    if (subscriptionOptional.isPresent()) {
      Subscription subscription = subscriptionOptional.get();

      return new ListUserDto()
          .id(user.getId())
          .fullName(user.getFullName())
          .role(user.getRole().name())
          .subscriptionStatus(subscription.getStatus())
          .endDate(OffsetDateTime.from(subscription.getEndDate()));
    }
    return new ListUserDto()
        .id(user.getId())
        .fullName(user.getFullName())
        .role(user.getRole().name());
  }

  @Named("userToSubscriptionDto")
  protected SubscriptionResponseDto userToSubscriptionDto(User user) {
    if (user == null || user.getId() == null) {
      return null;
    }
    Optional<Subscription> subscriptionOptional = user.getSubscriptions().stream()
        .max(Comparator.comparing(Subscription::getStartDate));
    return subscriptionOptional.map(subscriptionMapper::toSubscriptionResponseDto).orElse(null);
  }
}
