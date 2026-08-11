package ru.maxow.mvpn.promocode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreatePromocodeRequestDto;
import ru.maxow.mvpn.model.PageListPromocodeDto;
import ru.maxow.mvpn.model.PromocodeResponseDto;
import ru.maxow.mvpn.model.PromocodeStatsDto;
import ru.maxow.mvpn.model.PromocodeStatus;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static ru.maxow.mvpn.promocode.PromocodeSpecifications.codeContains;
import static ru.maxow.mvpn.promocode.PromocodeSpecifications.hasStatus;
import static ru.maxow.mvpn.util.PaginationUtils.parseSorting;

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
  @Transactional(readOnly = true)
  public PageListPromocodeDto getPromocodes(Integer page, Integer size, List<String> sort,
                                            String status, String search) {
    Specification<Promocode> spec = Specification.where(hasStatus(status))
        .and(codeContains(search));

    Page<Promocode> promocodes = promocodeRepository.findAll(spec, PageRequest.of(
        page, size, parseSorting(sort)));

    return new PageListPromocodeDto()
        .content(promocodes.getContent().stream().map(promocodeMapper::toDto).toList())
        .totalElements(promocodes.getTotalElements())
        .totalPages(promocodes.getTotalPages())
        .size(promocodes.getSize())
        .number(promocodes.getNumber());
  }

  @Override
  @Transactional(readOnly = true)
  public PromocodeStatsDto getPromocodeStats() {
    return new PromocodeStatsDto()
        .totalCodes(promocodeRepository.count())
        .activeCodes(promocodeRepository.countByStatus(PromocodeStatus.ACTIVE))
        .totalUsages(promocodeRepository.sumUsage());
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
