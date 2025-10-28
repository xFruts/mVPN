package ru.maxow.mvpn.tariff;

import ru.maxow.mvpn.tariff.dto.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.tariff.dto.TariffPlanResponseDto;

import java.util.List;

public interface TariffPlanService {
  TariffPlanResponseDto createTariffPlan(CreateUpdateRequestTariffPlanDto dto);

  TariffPlanResponseDto updateTariffPlan(Long id, CreateUpdateRequestTariffPlanDto dto);

  List<TariffPlanResponseDto> getAllTariffPlans();

  TariffPlanResponseDto getTariffPlanById(Long id);

  void deleteTariffPlan(Long id);
}
