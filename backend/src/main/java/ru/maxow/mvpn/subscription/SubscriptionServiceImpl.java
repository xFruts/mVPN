package ru.maxow.mvpn.subscription;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.subscription.dto.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.subscription.dto.SubscriptionResponseDto;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionServiceImpl implements SubscriptionService {

  SubscriptionRepository subscriptionRepository;
  SubscriptionMapper subscriptionMapper;
  UserRepository userRepository;

  @Override
  @Transactional
  public SubscriptionResponseDto createSubscription(
      Long userId, CreateUpdateSubscriptionDto dto) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User", userId));

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setStartDate(LocalDateTime.now());
    subscription.setEndDate(LocalDateTime.now().plusMonths(1));
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    Subscription savedSubscription = subscriptionRepository.save(subscription);
    log.info("subscription created for user with id: {}", userId);
    return subscriptionMapper.toSubscriptionResponseDto(savedSubscription);
  }

  @Override
  @Transactional
  public SubscriptionResponseDto updateSubscription(
      Long id, CreateUpdateSubscriptionDto dto) {
    Subscription subscription = findSubscriptionByIdInternal(id);
    subscriptionMapper.updateSubscriptionFromDto(dto, subscription);
    Subscription updatedSubscription = subscriptionRepository.save(subscription);
    log.info("Subscription with id: {} updated", id);
    return subscriptionMapper.toSubscriptionResponseDto(updatedSubscription);
  }

  @Override
  @Transactional
  public void deleteSubscription(Long id) {
    if (!subscriptionRepository.existsById(id)) {
      throw new NotFoundException("Subscription", id);
    }
    subscriptionRepository.deleteById(id);
    log.info("Subscription with id: {} deleted", id);
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionResponseDto findSubscriptionById(Long id) {
    return subscriptionRepository.findById(id)
        .map(subscriptionMapper::toSubscriptionResponseDto)
        .orElseThrow(() -> new NotFoundException("Subscription", id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<SubscriptionResponseDto> findSubscriptionsByUserId(Long userId) {
    return subscriptionRepository.findByUser_Id(userId).stream()
        .map(subscriptionMapper::toSubscriptionResponseDto)
        .toList();
  }

  @Override
  public void extendSubscription(Long userId, Integer billingMonth) {
    Subscription subscription = findSubscriptionByIdInternal(userId);

    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setEndDate(LocalDateTime.now().plusMonths(billingMonth));
    log.info("Subscription with id: {} extended", userId);
    subscriptionRepository.save(subscription);
  }

  private Subscription findSubscriptionByIdInternal(Long id) {
    return subscriptionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Subscription", id));
  }
}
