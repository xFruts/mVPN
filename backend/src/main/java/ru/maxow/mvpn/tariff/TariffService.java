package ru.maxow.mvpn.tariff;


import ru.maxow.mvpn.model.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.model.TariffPlanResponseDto;

import java.util.List;

public interface TariffService {
  TariffPlanResponseDto createTariffPlan(CreateUpdateRequestTariffPlanDto dto);

  TariffPlanResponseDto updateTariffPlan(Long id, CreateUpdateRequestTariffPlanDto dto);

  List<TariffPlanResponseDto> getAllTariffPlans();

  TariffPlanResponseDto getTariffPlanById(Long id);

  void deleteTariffPlan(Long id);
}
