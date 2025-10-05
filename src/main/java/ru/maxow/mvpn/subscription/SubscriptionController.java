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
import ru.maxow.mvpn.vpnconfig.VpnConfigResponseDto;

/**
 * Controller for managing subscriptions.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("v1/subscriptions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController {

  SubscriptionService subscriptionService;

  /**
   * Create a new subscription for a user.
   *
   * @param userId                  the ID of the user
   * @param subscriptionRequestDto  the subscription details
   * @return the created subscription
   */
  @PostMapping("/user/{userId}")
  public ResponseEntity<SubscriptionResponseDto> createSubscription(
      @PathVariable Long userId, @RequestBody @Validated SubscriptionRequestDto subscriptionRequestDto) {
    SubscriptionResponseDto createdSubscription = subscriptionService.createSubscription(userId, subscriptionRequestDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdSubscription);
  }

  /**
   * Get a subscription by its ID.
   *
   * @param id the ID of the subscription
   * @return the subscription details
   */
  @GetMapping("/{id}")
  public ResponseEntity<SubscriptionResponseDto> getSubscriptionById(@PathVariable Long id) {
    return ResponseEntity.ok(subscriptionService.findSubscriptionById(id));
  }

  /**
   * Get all subscriptions for a specific user.
   *
   * @param userId the ID of the user
   * @return list of subscriptions for the user
   */
  @GetMapping("/user/{userId}")
  public ResponseEntity<List<SubscriptionResponseDto>> getSubscriptionsByUserId(@PathVariable Long userId) {
    return ResponseEntity.ok(subscriptionService.findSubscriptionsByUserId(userId));
  }

  /**
   * Update an existing subscription.
   *
   * @param id                      the ID of the subscription to update
   * @param subscriptionRequestDto  the updated subscription details
   * @return the updated subscription
   */
  @PutMapping("/{id}")
  public ResponseEntity<SubscriptionResponseDto> updateSubscription(
      @PathVariable Long id, @RequestBody @Validated SubscriptionRequestDto subscriptionRequestDto) {
    return ResponseEntity.ok(subscriptionService.updateSubscription(id, subscriptionRequestDto));
  }

  /**
   * Delete a subscription by its ID.
   *
   * @param id the ID of the subscription to delete
   * @return response entity with no content
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
    subscriptionService.deleteSubscription(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Add a VPN configuration to a subscription.
   *
   * @param subscriptionId the ID of the subscription
   * @param protocol       the VPN protocol
   * @param file           the VPN configuration file (optional)
   * @param link           the link to the VPN configuration (optional)
   * @return the added VPN configuration details
   */
  @PostMapping("/{subscriptionId}/configs")
  public ResponseEntity<VpnConfigResponseDto> addVpnConfig(
      @PathVariable Long subscriptionId,
      @RequestParam("protocol") Protocol protocol,
      @RequestParam(value = "file", required = false) MultipartFile file,
      @RequestParam(value = "link", required = false) String link) {
    VpnConfigResponseDto newConfig = subscriptionService.addVpnConfigToSubscription(
        subscriptionId, protocol, file, link);
    return ResponseEntity.status(HttpStatus.CREATED).body(newConfig);
  }
}
