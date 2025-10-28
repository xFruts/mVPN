package ru.maxow.mvpn.tariff;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.server.ServerService;
import ru.maxow.mvpn.tariff.dto.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.tariff.dto.TariffPlanResponseDto;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TariffPlanServiceImpl implements TariffPlanService {

  TariffPlanRepository tariffPlanRepository;
  TariffPlanMapper tariffPlanMapper;
  ServerService serverService;

  @Override
  @Transactional
  public TariffPlanResponseDto createTariffPlan(CreateUpdateRequestTariffPlanDto dto) {
    TariffPlan tariffPlan = new TariffPlan();
    tariffPlanMapper.updateFromDto(dto, tariffPlan);

    tariffPlan.setServers(serverService.getServersById(dto.serverIds()));
    TariffPlan createdTariffPlan = tariffPlanRepository.save(tariffPlan);
    return tariffPlanMapper.toResponseDto(createdTariffPlan);
  }

  @Override
  @Transactional
  public TariffPlanResponseDto updateTariffPlan(Long id, CreateUpdateRequestTariffPlanDto dto) {
    TariffPlan existingTariffPlan = tariffPlanRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("TariffPlan", id));

    tariffPlanMapper.updateFromDto(dto, existingTariffPlan);
    TariffPlan updatedTariffPlan = tariffPlanRepository.save(existingTariffPlan);
    return  tariffPlanMapper.toResponseDto(updatedTariffPlan);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TariffPlanResponseDto> getAllTariffPlans() {
    return tariffPlanRepository.findAll().stream()
        .map(tariffPlanMapper::toResponseDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public TariffPlanResponseDto getTariffPlanById(Long id) {
    return tariffPlanRepository.findById(id)
        .map(tariffPlanMapper::toResponseDto)
        .orElseThrow(() -> new NotFoundException("TariffPlan", id));
  }

  @Override
  @Transactional
  public void deleteTariffPlan(Long id) {
    if (tariffPlanRepository.existsById(id)) {
      tariffPlanRepository.deleteById(id);
    } else {
      throw new NotFoundException("TariffPlan", id);
    }
  }
}
