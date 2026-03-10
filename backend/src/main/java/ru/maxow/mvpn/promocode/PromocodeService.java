package ru.maxow.mvpn.promocode;

import ru.maxow.mvpn.model.CreatePromocodeRequestDto;
import ru.maxow.mvpn.model.PromocodeResponseDto;

import java.util.List;

public interface PromocodeService {
  PromocodeResponseDto createPromocode(CreatePromocodeRequestDto requestDto);

  List<PromocodeResponseDto> getPromocodes();

  PromocodeResponseDto usePromocode(String code);

  void deletePromocode(Long id);
}
