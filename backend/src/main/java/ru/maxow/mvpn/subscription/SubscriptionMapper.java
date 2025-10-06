package ru.maxow.mvpn.subscription;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for converting between Subscription entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

  /**
   * Converts a Subscription to a SubscriptionResponseDto.
   *
   * @param subscription the source Entity
   * @return the mapped SubscriptionResponseDto entity
   */
  @Mapping(source = "user.id", target = "userId")
  SubscriptionResponseDto toSubscriptionResponseDto(Subscription subscription);

  /**
   * Updates an existing Subscription entity with data from a SubscriptionRequestDto.
   * Ignores fields that should not be updated directly.
   *
   * @param dto    the source DTO containing updated data
   * @param entity the target Subscription entity to be updated
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "vpnConfigs", ignore = true)
  @Mapping(target = "startDate", ignore = true)
  @Mapping(target = "endDate", ignore = true)
  @Mapping(target = "status", ignore = true)
  void updateSubscriptionFromDto(SubscriptionRequestDto dto, @MappingTarget Subscription entity);
}
