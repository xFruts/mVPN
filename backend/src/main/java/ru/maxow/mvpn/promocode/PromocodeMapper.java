package ru.maxow.mvpn.promocode;

import org.mapstruct.Mapper;
import ru.maxow.mvpn.model.PromocodeResponseDto;
import ru.maxow.mvpn.tariff.TariffMapper;

@Mapper(componentModel = "spring", uses = TariffMapper.class)
public interface PromocodeMapper {
  PromocodeResponseDto toDto(Promocode promoCode);
}
