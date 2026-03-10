package ru.maxow.mvpn.subscription;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.SubscriptionsApi;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionController implements SubscriptionsApi {

  SubscriptionService subscriptionService;

  @Override
  public SubscriptionResponseDto v1SubscriptionsUserUserIdPost(
      Long userId,
      CreateUpdateSubscriptionDto createUpdateSubscriptionDto) {
    return subscriptionService.createSubscription(userId, createUpdateSubscriptionDto);
  }

  @Override
  public SubscriptionResponseDto v1SubscriptionsIdGet(Long id) {
    return subscriptionService.findSubscriptionById(id);
  }

  @Override
  public List<SubscriptionResponseDto> v1SubscriptionsUserUserIdGet(Long userId) {
    return subscriptionService.findSubscriptionsByUserId(userId);
  }

  @Override
  public SubscriptionResponseDto v1SubscriptionsIdPut(
      Long id,
      CreateUpdateSubscriptionDto createUpdateSubscriptionDto) {
    return subscriptionService.updateSubscription(id, createUpdateSubscriptionDto);
  }

  @Override
  public void v1SubscriptionsIdDelete(Long id) {
    subscriptionService.deleteSubscription(id);
  }
}
