package ru.maxow.mvpn.promocode;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@Validated
@RestController
@RequestMapping("v1/promocodes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromocodeController {

  PromocodeService promocodeService;

  @GetMapping
  ResponseEntity<List<PromocodeResponseDto>> getPromocodes() {
    return ResponseEntity.ok().body(promocodeService.getPromocodes());
  }

  @PostMapping
  ResponseEntity<PromocodeResponseDto> createPromocode(
      @RequestBody CreatePromocodeRequestDto requestDto) {
    PromocodeResponseDto createdPromocode = promocodeService.createPromocode(requestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdPromocode);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> deletePromocode(@PathVariable Long id) {
    promocodeService.deletePromocode(id);
    return ResponseEntity.noContent().build();
  }
}
