package ru.maxow.mvpn.subscription;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffRepository;
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
  TariffRepository tariffRepository;

  @Override
  @Transactional
  public SubscriptionResponseDto createSubscription(
      Long userId, CreateUpdateSubscriptionDto dto) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User", userId));

    Subscription subscription = new Subscription();
    subscription.setUser(user);

    subscription.setStartDate(dto.getStartDate());

    if (dto.getEndDate().isBefore(dto.getStartDate())) {
      throw new IllegalArgumentException("End date cannot be before start date");
    }

    subscription.setEndDate(dto.getEndDate());

    subscription.setStatus(dto.getStatus());

    Tariff tariff = tariffRepository.findById(dto.getTariffId())
        .orElseThrow(() -> new NotFoundException("Tariff", dto.getTariffId()));
    subscription.setTariff(tariff);

    Subscription savedSubscription = subscriptionRepository.save(subscription);
    log.info("subscription created for user with id: {}", userId);
    return subscriptionMapper.toSubscriptionResponseDto(savedSubscription);
  }

  @Override
  @Transactional
  public SubscriptionResponseDto updateSubscription(
      Long id, CreateUpdateSubscriptionDto dto) {
    Subscription subscription = subscriptionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Subscription", id));
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
  public void extendSubscription(Long userId) {
    Subscription subscription = subscriptionRepository
        .findFirstByUser_IdOrderByStartDateDesc(userId)
        .orElseThrow(() -> new NotFoundException("Subscription for user", userId));

    subscription.setStatus(SubscriptionStatus.ACTIVE);

    subscription.setEndDate(
        OffsetDateTime.now().plusDays(subscription.getTariff().getDurationOfDays()));

    log.info("Subscription with user_id: {} extended", userId);
    subscriptionRepository.save(subscription);
  }
}
