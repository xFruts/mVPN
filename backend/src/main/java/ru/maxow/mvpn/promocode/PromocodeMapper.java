package ru.maxow.mvpn.promocode;

import org.mapstruct.Mapper;
import ru.maxow.mvpn.model.PromocodeResponseDto;

@Mapper(componentModel = "spring")
public interface PromocodeMapper {
  PromocodeResponseDto toDto(Promocode promoCode);
}
