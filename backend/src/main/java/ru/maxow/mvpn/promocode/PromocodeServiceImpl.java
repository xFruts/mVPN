package ru.maxow.mvpn.promocode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreatePromocodeRequestDto;
import ru.maxow.mvpn.model.PromocodeResponseDto;
import ru.maxow.mvpn.model.PromocodeStatus;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromocodeServiceImpl implements  PromocodeService {

  PromocodeRepository promocodeRepository;
  PromocodeMapper promocodeMapper;
  TariffRepository tariffRepository;

  @Override
  public PromocodeResponseDto createPromocode(CreatePromocodeRequestDto requestDto) {
    Promocode promocode = new Promocode();
    promocode.setCode(generateUniqueCode());
    promocode.setUsageLimit(requestDto.getUsageLimit());
    promocode.setExpirationDate(OffsetDateTime.now().plusDays(requestDto.getValidDays()));
    promocode.setStatus(PromocodeStatus.ACTIVE);

    Tariff tariff = tariffRepository.findById(requestDto.getTariffId())
        .orElseThrow(() -> new NotFoundException("Tariff", requestDto.getTariffId()));
    promocode.setTariff(tariff);

    Promocode savedPromocode = promocodeRepository.save(promocode);
    log.info("Promocode save with id: {}", savedPromocode.getId());

    return promocodeMapper.toDto(savedPromocode);
  }

  private String generateUniqueCode() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  @Override
  public List<PromocodeResponseDto> getPromocodes() {
    return promocodeRepository.findAll().stream()
        .map(promocodeMapper::toDto)
        .toList();
  }

  @Override
  @Transactional
  public PromocodeResponseDto usePromocode(String code) {
    Promocode promocode = promocodeRepository.findByCode(code)
        .orElseThrow(() -> new NotFoundException("Promocode"));

    if (promocode.getStatus() != PromocodeStatus.ACTIVE) {
      throw new BadRequestException("Promocode is not active");
    }

    if (promocode.getExpirationDate().isBefore(OffsetDateTime.now())) {
      promocode.setStatus(PromocodeStatus.EXPIRED);
      promocodeRepository.save(promocode);
      throw new BadRequestException("Promocode is expired");
    }
    if (promocode.getUsage() >= promocode.getUsageLimit()) {
      promocode.setStatus(PromocodeStatus.USED);
      promocodeRepository.save(promocode);
      throw new BadRequestException("Promocode usage limit reached");
    }
    promocode.setUsage(promocode.getUsage() + 1);
    if (promocode.getUsage() >= promocode.getUsageLimit()) {
      promocode.setStatus(PromocodeStatus.USED);
    }

    return promocodeMapper.toDto(promocodeRepository.save(promocode));
  }

  @Override
  public void deletePromocode(Long id) {
    try {
      promocodeRepository.deleteById(id);
      log.info("Delete promocode with id: {}", id);
    } catch (Exception e) {
      log.error("Error deleting promocode with id {}: {}", id, e.getMessage());
      throw new NotFoundException("Promocode", id);
    }
  }
}
