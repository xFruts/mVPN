package ru.maxow.mvpn.promocode;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromocodeMapper {
  PromocodeResponseDto toDto(Promocode promoCode);
}
