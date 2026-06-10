package ru.maxow.mvpn.subscription;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.SubscriptionsApi;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.ExtendSubscriptionsRequestDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController implements SubscriptionsApi {

  SubscriptionService subscriptionService;

  @Override
  public SubscriptionResponseDto createSubscription(
      Long userId,
      CreateUpdateSubscriptionDto createUpdateSubscriptionDto) {
    return subscriptionService.createSubscription(userId, createUpdateSubscriptionDto);
  }

  @Override
  public SubscriptionResponseDto getSubscriptionById(Long id) {
    return subscriptionService.findSubscriptionById(id);
  }

  @Override
  public List<SubscriptionResponseDto> getSubscriptionsByUserId(Long userId) {
    return subscriptionService.findSubscriptionsByUserId(userId);
  }

  @Override
  public SubscriptionResponseDto updateSubscription(
      Long id,
      CreateUpdateSubscriptionDto createUpdateSubscriptionDto) {
    return subscriptionService.updateSubscription(id, createUpdateSubscriptionDto);
  }

  @Override
  public void deleteSubscription(Long id) {
    subscriptionService.deleteSubscription(id);
  }

  @Override
  public SubscriptionResponseDto getLastSubscription(Long userId) {
    return subscriptionService.getLastSubscriptionByUserId(userId);
  }

  @Override
  public void extendSubscriptions(
      ExtendSubscriptionsRequestDto extendSubscriptionsRequestDto) {
    subscriptionService.extendSubscriptionsByUserIds(
        extendSubscriptionsRequestDto.getUserIds());
  }
}
