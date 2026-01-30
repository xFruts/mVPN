package ru.maxow.mvpn.subscription;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.maxow.mvpn.subscription.dto.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.subscription.dto.SubscriptionResponseDto;

@Slf4j
@Validated
@RestController
@RequestMapping("v1/subscriptions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController {

  SubscriptionService subscriptionService;

  @PostMapping("/user/{userId}")
  public ResponseEntity<SubscriptionResponseDto> createSubscription(
      @PathVariable Long userId,
      @RequestBody @Validated CreateUpdateSubscriptionDto subscriptionRequestDto) {
    SubscriptionResponseDto createdSubscription =
        subscriptionService.createSubscription(userId, subscriptionRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdSubscription);
  }

  @GetMapping("/{id}")
  public ResponseEntity<SubscriptionResponseDto> getSubscriptionById(@PathVariable Long id) {
    return ResponseEntity.ok(subscriptionService.findSubscriptionById(id));
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<List<SubscriptionResponseDto>> getSubscriptionsByUserId(
      @PathVariable Long userId) {
    return ResponseEntity.ok(subscriptionService.findSubscriptionsByUserId(userId));
  }

  @PutMapping("/{id}")
  public ResponseEntity<SubscriptionResponseDto> updateSubscription(
      @PathVariable Long id,
      @RequestBody @Validated CreateUpdateSubscriptionDto subscriptionRequestDto) {
    return ResponseEntity.ok(subscriptionService.updateSubscription(id, subscriptionRequestDto));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
    subscriptionService.deleteSubscription(id);
    return ResponseEntity.noContent().build();
  }
}
