package ru.maxow.mvpn.tariff;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.maxow.mvpn.tariff.dto.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.tariff.dto.TariffPlanResponseDto;

import java.util.List;

@RestController
@RequestMapping("v1/tariffs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TariffController {

  TariffService tariffPlanService;

  @GetMapping
  ResponseEntity<List<TariffPlanResponseDto>> getAllTariffPlans() {
    return ResponseEntity.ok(tariffPlanService.getAllTariffPlans());
  }

  @GetMapping("/{id}")
  ResponseEntity<TariffPlanResponseDto> getTariffPlanById(@PathVariable Long id) {
    return ResponseEntity.ok(tariffPlanService.getTariffPlanById(id));
  }

  @PostMapping
  ResponseEntity<TariffPlanResponseDto> createTariffPlan(@RequestBody CreateUpdateRequestTariffPlanDto dto) {
    return ResponseEntity.ok(tariffPlanService.createTariffPlan(dto));
  }

  @PutMapping("/{id}")
  ResponseEntity<TariffPlanResponseDto> updateTariffPlan(
      @PathVariable Long id, @RequestBody CreateUpdateRequestTariffPlanDto dto) {
    return ResponseEntity.ok(tariffPlanService.updateTariffPlan(id, dto));
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteTariffPlan(@PathVariable Long id) {
    tariffPlanService.deleteTariffPlan(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

}
