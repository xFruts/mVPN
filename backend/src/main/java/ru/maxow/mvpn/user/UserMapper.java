package ru.maxow.mvpn.user;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import ru.maxow.mvpn.subscription.Protocol;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionMapper;
import ru.maxow.mvpn.subscription.SubscriptionResponseDto;
import ru.maxow.mvpn.user.dto.CreateUserRequestDto;
import ru.maxow.mvpn.user.dto.ListUserDto;
import ru.maxow.mvpn.user.dto.UserResponseDto;
import ru.maxow.mvpn.vpnconfig.AmneziaWgConfig;
import ru.maxow.mvpn.vpnconfig.VpnConfig;
import ru.maxow.mvpn.vpnconfig.XrayConfig;


/**  Mapper for converting between User entity and DTOs.*/
@Mapper(componentModel = "spring")
public abstract class UserMapper {

  @Autowired
  SubscriptionMapper subscriptionMapper;

  /** Converts a User entity to a UserResponseDto. */
  @Mapping(source = "user", target = "subscription", qualifiedByName = "userToSubscriptionDto")
  @Mapping(source = "role", target = "role")
  public abstract UserResponseDto toUserResponseDto(User user);

  /** Converts a CreateUserRequestDto to a User entity. */
  public abstract User toUser(CreateUserRequestDto userRequestDto);

  /** Converts a User entity to a ListUserDto. */
  public ListUserDto toListUserDto(User user) {
    Optional<Subscription> subscriptionOptional = user.getSubscriptions().stream()
        .max(Comparator.comparing(Subscription::getStartDate));

    if (subscriptionOptional.isPresent()) {
      Subscription subscription = subscriptionOptional.get();
      List<Protocol> protocols = subscription.getVpnConfigs().stream()
          .map(this::getProtocolFromConfig)
          .distinct()
          .toList();

      return new ListUserDto(
          user.getId(),
          user.getFullName(),
          user.getRole().name(),
          subscription.getType(),
          subscription.getStatus(),
          protocols,
          subscription.getEndDate()
      );
    }
    return new ListUserDto(
        user.getId(),
        user.getFullName(),
        user.getRole().name(),
        null,
        null,
        Collections.emptyList(),
        null
    );
  }

  private Protocol getProtocolFromConfig(VpnConfig vpnConfig) {
    if (vpnConfig instanceof AmneziaWgConfig) {
      return Protocol.AMNEZIA_WG;
    } else if (vpnConfig instanceof XrayConfig) {
      return Protocol.XRAY;
    }
    throw new IllegalStateException("Unknown VpnConfig type: " + vpnConfig.getClass().getName());
  }

  /** Maps the most recent Subscription of a User to a SubscriptionResponseDto. */
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
