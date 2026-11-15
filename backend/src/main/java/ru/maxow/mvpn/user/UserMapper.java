package ru.maxow.mvpn.user;

import java.util.Comparator;
import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.ListUserDto;
import ru.maxow.mvpn.model.ShortListUserDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionMapper;
import ru.maxow.mvpn.tariff.TariffMapper;

/**
 * MapStruct маппер для преобразования сущностей {@link User} в DTO.
 */
@Mapper(componentModel = "spring", uses = {SubscriptionMapper.class, TariffMapper.class})
public abstract class UserMapper {

  @Mapping(source = "role", target = "role")
  @Mapping(target = "subscription", ignore = true)
  public abstract UserResponseDto toUserResponseDto(User user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "verificationCode", ignore = true)
  @Mapping(target = "subscriptions", ignore = true)
  @Mapping(target = "xuiId", ignore = true)
  @Mapping(target = "xuiSubscription", ignore = true)
  @Mapping(target = "deleted", ignore = true)
  public abstract User toUser(CreateUserRequestDto userRequestDto);

  public abstract ShortListUserDto toShortListUserDto(User user);

  @Mapping(target = "subscriptionStatus", ignore = true)
  @Mapping(target = "endDate", ignore = true)
  @Mapping(target = "tariffName", ignore = true)
  public abstract ListUserDto toListUserDto(User user);

  /**
   * Обогащает DTO пользователя подробной информацией об актуальной подписке.
   */
  @AfterMapping
  protected void enrichUserResponseDto(User user, @MappingTarget UserResponseDto dto) {
    if (user != null && user.getId() != null) {
      findLatestSubscription(user)
          .map(this::toSubscriptionResponseDto)
          .ifPresent(dto::setSubscription);
    }
  }

  /**
   * Обогащает элемент списка пользователей статусом, датой окончания и названием
   * тарифа последней подписки для компактного отображения в таблице админки.
   */
  @AfterMapping
  protected void enrichListUserDto(User user, @MappingTarget ListUserDto dto) {
    if (user != null) {
      findLatestSubscription(user).ifPresent(sub -> {
        dto.subscriptionStatus(sub.getStatus())
            .endDate(sub.getEndDate())
            .tariffName(sub.getTariff() != null ? sub.getTariff().getName() : null);
      });
    }
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
