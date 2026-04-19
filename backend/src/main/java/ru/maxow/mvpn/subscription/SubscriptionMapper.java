package ru.maxow.mvpn.subscription;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.BeanMapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.tariff.TariffMapper;

@Mapper(componentModel = "spring", uses = TariffMapper.class)
public interface SubscriptionMapper {

  @Mapping(source = "user.id", target = "userId")
  SubscriptionResponseDto toSubscriptionResponseDto(Subscription subscription);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "tariff",  ignore = true)
  @Mapping(target = "endDate", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateSubscriptionFromDto(CreateUpdateSubscriptionDto dto, @MappingTarget Subscription entity);
}
