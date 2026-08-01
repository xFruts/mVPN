package ru.maxow.mvpn.promocode;

import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreatePromocodeRequestDto;
import ru.maxow.mvpn.model.PageListPromocodeDto;
import ru.maxow.mvpn.model.PromocodeResponseDto;

import java.util.List;

public interface PromocodeService {
  PromocodeResponseDto createPromocode(CreatePromocodeRequestDto requestDto);

  @Transactional(readOnly = true)
  PageListPromocodeDto getPromocodes(Integer page, Integer size, List<String> sort,
                                     String status, String search);

  PromocodeResponseDto usePromocode(String code);

  void deletePromocode(Long id);
}
