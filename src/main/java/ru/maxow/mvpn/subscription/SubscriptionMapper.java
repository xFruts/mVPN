package ru.maxow.mvpn.subscription;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

  SubscriptionResponseDto toSubscriptionResponseDto(Subscription subscription);

}
