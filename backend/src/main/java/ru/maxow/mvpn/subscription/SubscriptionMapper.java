package ru.maxow.mvpn.subscription;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

  @Mapping(source = "user.id", target = "userId")
  SubscriptionResponseDto toSubscriptionResponseDto(Subscription subscription);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  void updateSubscriptionFromDto(CreateUpdateSubscriptionDto dto, @MappingTarget Subscription entity);
}
