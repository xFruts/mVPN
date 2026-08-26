package ru.maxow.mvpn.user;

import java.util.Comparator;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.ListUserDto;
import ru.maxow.mvpn.model.ShortListUserDto;
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
  @Mapping(target = "deleted", ignore = true)
  public abstract User toUser(CreateUserRequestDto userRequestDto);

  public abstract ShortListUserDto toShortListUserDto(User user);

  public ListUserDto toListUserDto(User user) {
    if (user == null) {
      return null;
    }

    Optional<Subscription> latestSubscription = findLatestSubscription(user);

    ListUserDto dto = new ListUserDto()
        .id(user.getId())
        .fullName(user.getFullName())
        .role(user.getRole() != null ? user.getRole().name() : null);

    latestSubscription.ifPresent(subscription -> dto.subscriptionStatus(subscription.getStatus())
        .endDate(subscription.getEndDate())
        .tariffName(subscription.getTariff() != null ? subscription.getTariff().getName() : null));

    return dto;
  }

  @Named("userToSubscriptionDto")
  protected SubscriptionResponseDto userToSubscriptionDto(User user) {
    if (user == null || user.getId() == null) {
      return null;
    }

    return findLatestSubscription(user)
        .map(this::toSubscriptionResponseDto)
        .orElse(null);
  }

  @Mapping(target = "userId", source = "user.id")
  protected abstract SubscriptionResponseDto toSubscriptionResponseDto(Subscription subscription);

  private Optional<Subscription> findLatestSubscription(User user) {
    if (user.getSubscriptions() == null || user.getSubscriptions().isEmpty()) {
      return Optional.empty();
    }

    return user.getSubscriptions().stream()
        .filter(subscription -> subscription.getStartDate() != null)
        .max(Comparator.comparing(Subscription::getStartDate));
  }
}
