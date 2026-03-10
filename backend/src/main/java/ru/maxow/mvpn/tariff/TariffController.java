package ru.maxow.mvpn.tariff;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.TariffsApi;
import ru.maxow.mvpn.model.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.model.TariffPlanResponseDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TariffController implements TariffsApi {

  TariffService tariffPlanService;

  @Override
  public List<TariffPlanResponseDto> v1TariffsGet() {
    return tariffPlanService.getAllTariffPlans();
  }

  @Override
  public TariffPlanResponseDto v1TariffsIdGet(Long id) {
    return tariffPlanService.getTariffPlanById(id);
  }

  @Override
  public TariffPlanResponseDto v1TariffsPost(CreateUpdateRequestTariffPlanDto createUpdateRequestTariffPlanDto) {
    return tariffPlanService.createTariffPlan(createUpdateRequestTariffPlanDto);
  }

  @Override
  public TariffPlanResponseDto v1TariffsIdPut(Long id, CreateUpdateRequestTariffPlanDto createUpdateRequestTariffPlanDto) {
    return tariffPlanService.updateTariffPlan(id, createUpdateRequestTariffPlanDto);
  }

  @Override
  public void v1TariffsIdDelete(Long id) {
    tariffPlanService.deleteTariffPlan(id);
  }

}
