package ru.maxow.mvpn.tariff;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.model.TariffPlanResponseDto;
import ru.maxow.mvpn.server.ServerService;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TariffServiceImpl implements TariffService {

  TariffRepository tariffPlanRepository;
  TariffMapper tariffMapper;
  ServerService serverService;

  @Override
  @Transactional
  public TariffPlanResponseDto createTariffPlan(CreateUpdateRequestTariffPlanDto dto) {
    Tariff tariff = new Tariff();
    tariffMapper.updateFromDto(dto, tariff);

    tariff.setServers(serverService.getServersById(dto.getServerIds()));
    Tariff createdTariff = tariffPlanRepository.save(tariff);
    return tariffMapper.toResponseDto(createdTariff);
  }

  @Override
  @Transactional
  public TariffPlanResponseDto updateTariffPlan(Long id, CreateUpdateRequestTariffPlanDto dto) {
    Tariff existingTariff = tariffPlanRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Tariff", id));

    tariffMapper.updateFromDto(dto, existingTariff);
    Tariff updatedTariff = tariffPlanRepository.save(existingTariff);
    return  tariffMapper.toResponseDto(updatedTariff);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TariffPlanResponseDto> getAllTariffPlans() {
    return tariffPlanRepository.findAll().stream()
        .map(tariffMapper::toResponseDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public TariffPlanResponseDto getTariffPlanById(Long id) {
    return tariffPlanRepository.findById(id)
        .map(tariffMapper::toResponseDto)
        .orElseThrow(() -> new NotFoundException("Tariff", id));
  }

  @Override
  @Transactional
  public void deleteTariffPlan(Long id) {
    if (tariffPlanRepository.existsById(id)) {
      tariffPlanRepository.deleteById(id);
    } else {
      throw new NotFoundException("Tariff", id);
    }
  }
}
