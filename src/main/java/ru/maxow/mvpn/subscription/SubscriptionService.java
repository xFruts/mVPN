package ru.maxow.mvpn.subscription;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import ru.maxow.mvpn.vpnconfig.VpnConfigResponseDto;

/**
 * Service interface for managing subscriptions.
 */
public interface SubscriptionService {
  /**
   * Creates a new subscription for a user.
   *
   * @param userId the ID of the user
   * @param subscriptionRequestDto the subscription request data
   * @return the created subscription response data
   */
  SubscriptionResponseDto createSubscription(Long userId, SubscriptionRequestDto subscriptionRequestDto);

  /**
   * Updates an existing subscription.
   *
   * @param id the ID of the subscription to update
   * @param subscriptionRequestDto the updated subscription data
   * @return the updated subscription response data
   */
  SubscriptionResponseDto updateSubscription(Long id, SubscriptionRequestDto subscriptionRequestDto);

  /**
   * Deletes a subscription by its ID.
   *
   * @param id the ID of the subscription to delete
   */
  void deleteSubscription(Long id);

  /**
   * Finds a subscription by its ID.
   *
   * @param id the ID of the subscription to find
   * @return the found subscription response data
   */
  SubscriptionResponseDto findSubscriptionById(Long id);

  /**
   * Finds all subscriptions for a given user ID.
   *
   * @param id the ID of the user
   * @return a list of subscription response data
   */
  List<SubscriptionResponseDto> findSubscriptionsByUserId(Long id);

  /**
   * Adds a VPN configuration to a subscription.
   *
   * @param subscriptionId the ID of the subscription
   * @param protocol the VPN protocol
   * @param file the VPN configuration file
   * @param link an optional link for the VPN configuration
   * @return the added VPN configuration response data
   */
  VpnConfigResponseDto addVpnConfigToSubscription(Long subscriptionId, Protocol protocol, MultipartFile file, String link);
}
