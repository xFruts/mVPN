package ru.maxow.mvpn.promocode;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.PromocodesApi;
import ru.maxow.mvpn.model.CreatePromocodeRequestDto;
import ru.maxow.mvpn.model.PromocodeResponseDto;


@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromocodeController implements PromocodesApi {

  PromocodeService promocodeService;

  @Override
  public List<PromocodeResponseDto> getPromocodes() {
    return promocodeService.getPromocodes();
  }

  @Override
  public PromocodeResponseDto createPromocode(CreatePromocodeRequestDto createPromocodeRequestDto) {
    return promocodeService.createPromocode(createPromocodeRequestDto);
  }

  @Override
  public void deletePromocode(Long id) {
    promocodeService.deletePromocode(id);
  }
}
