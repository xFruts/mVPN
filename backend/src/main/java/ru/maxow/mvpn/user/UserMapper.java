package ru.maxow.mvpn.user;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionMapper;
import ru.maxow.mvpn.subscription.dto.SubscriptionResponseDto;
import ru.maxow.mvpn.user.dto.CreateUserRequestDto;
import ru.maxow.mvpn.user.dto.ListUserDto;
import ru.maxow.mvpn.user.dto.UserResponseDto;

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

      return new ListUserDto(
          user.getId(),
          user.getFullName(),
          user.getRole().name(),
          subscription.getStatus(),
          subscription.getEndDate()
      );
    }
    return new ListUserDto(
        user.getId(),
        user.getFullName(),
        user.getRole().name()
        , null, null
    );
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
