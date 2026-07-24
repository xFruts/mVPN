package ru.maxow.mvpn.subscription;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficStateService;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.BadRequestException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionServiceImpl implements SubscriptionService {

  SubscriptionRepository subscriptionRepository;
  SubscriptionMapper subscriptionMapper;
  UserRepository userRepository;
  TariffRepository tariffRepository;
  SubscriptionTrafficStateService trafficStateService;

  @Override
  @Transactional
  public SubscriptionResponseDto createSubscription(
      Long userId, CreateUpdateSubscriptionDto dto) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User", userId));

    Subscription subscription = new Subscription();
    subscription.setUser(user);

    Tariff tariff = tariffRepository.findById(dto.getTariffId())
        .orElseThrow(() -> new NotFoundException("Tariff", dto.getTariffId()));

    OffsetDateTime startDate = dto.getStartDate() != null
        ? dto.getStartDate()
        : OffsetDateTime.now(ZoneOffset.UTC);

    subscription.setStartDate(startDate);
    subscription.setEndDate(startDate.plusDays(tariff.getDurationOfDays()));
    subscription.setStatus(dto.getStatus());
    subscription.setTariff(tariff);

    Subscription savedSubscription = subscriptionRepository.save(subscription);
    expireOverdueSubscriptions(
        subscriptionRepository.findByUser_IdOrderByStartDateDesc(userId));
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

    Tariff tariff = tariffRepository.findById(dto.getTariffId())
        .orElseThrow(() -> new NotFoundException("Tariff", dto.getTariffId()));

    OffsetDateTime effectiveStartDate = subscription.getStartDate();
    if (effectiveStartDate == null) {
      effectiveStartDate = OffsetDateTime.now(ZoneOffset.UTC);
      subscription.setStartDate(effectiveStartDate);
    }

    subscription.setTariff(tariff);
    subscription.setEndDate(effectiveStartDate.plusDays(tariff.getDurationOfDays()));

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
  @Transactional
  public List<SubscriptionResponseDto> findSubscriptionsByUserId(Long userId) {
    List<Subscription> subscriptions =
        subscriptionRepository.findByUser_IdOrderByStartDateDesc(userId);
    expireOverdueSubscriptions(subscriptions);
    return subscriptions.stream()
        .map(subscriptionMapper::toSubscriptionResponseDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public String getSubscriptionInfoForUserByCode(UUID verificationCode) {
    User user = userRepository.findByVerificationCode(verificationCode)
        .orElseThrow(() -> new NotFoundException("User by verification code"));

    Subscription subscription = subscriptionRepository
        .findFirstByUser_IdOrderByStartDateDesc(user.getId())
        .orElseThrow(() -> new NotFoundException("Subscription for user", user.getId()));

    SubscriptionTrafficState trafficState = trafficStateService
        .getTrafficStateBySubscriptionId(subscription.getId())
        .orElse(null);
    if (trafficState == null) {
      trafficState = trafficStateService.syncTrafficForSubscription(user, subscription);
    }

    Tariff tariff = subscription.getTariff();
    long trafficLimitBytes = tariff.getTrafficLimitGb() * 1024L * 1024L * 1024L;

    long expireSec = subscription.getEndDate().toInstant().getEpochSecond();

    return String.format(
        "upload=%d; download=%d; total=%d; expire=%d",
        trafficState.getUsedUploadBytes(),
        trafficState.getUsedDownloadBytes(),
        trafficLimitBytes,
        expireSec
    );
  }

  @Override
  @Transactional
  public void extendSubscription(Long userId, String paidUntilDate) {
    OffsetDateTime date;
    try {
      date = LocalDate.parse(paidUntilDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          .atStartOfDay()
          .atOffset(ZoneOffset.UTC);
    } catch (DateTimeParseException e) {
      log.warn("Invalid billing month format for user {}: {}", userId, paidUntilDate);
      throw new BadRequestException("Invalid billing date format. Expected YYYY-MM-DD format");
    }

    if (date.isBefore(OffsetDateTime.now())) {
      log.warn("Billing date is in the past for user {}: {}", userId, paidUntilDate);
      throw new BadRequestException("Billing month cannot be in the past");
    }

    Subscription subscription = subscriptionRepository
        .findFirstByUser_IdOrderByStartDateDesc(userId)
        .orElseThrow(() -> {
          log.warn("No subscription found for user {}", userId);
          return new BadRequestException("User has no active subscriptions");
        });

    subscription.setStatus(SubscriptionStatus.ACTIVE);

    OffsetDateTime newEndDate = subscription.getEndDate()
        .isAfter(date) ? subscription.getEndDate() : date;
    subscription.setEndDate(newEndDate);

    subscriptionRepository.save(subscription);
    log.info("Subscription for user {} extended until {}", userId, newEndDate);
  }

  @Override
  @Transactional(readOnly = true)
  public Subscription findLastSubscriptionEntityByUserId(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User", userId));

    return subscriptionRepository.findFirstWithTariffByUserOrderByStartDateDesc(user)
        .orElseThrow(() -> new NotFoundException("Subscription"));
  }

  @Override
  @Transactional(readOnly = true)
  public SubscriptionResponseDto getLastSubscriptionByUserId(Long userId) {
    return subscriptionMapper.toSubscriptionResponseDto(findLastSubscriptionEntityByUserId(userId));
  }

  @Override
  @Transactional
  public void extendSubscriptionsByUserIds(List<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      throw new BadRequestException("User IDs list cannot be empty");
    }

    LinkedHashSet<Long> uniqueUserIds = new LinkedHashSet<>(userIds);
    List<Subscription> subscriptions = new ArrayList<>(uniqueUserIds.size());
    List<Long> missingUserIds = new ArrayList<>();

    for (Long userId : uniqueUserIds) {
      if (userId == null) {
        throw new BadRequestException("User ID cannot be null");
      }
      subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId)
          .ifPresentOrElse(subscriptions::add, () -> missingUserIds.add(userId));
    }

    if (!missingUserIds.isEmpty()) {
      throw new NotFoundException("No subscription found for user(s): " + missingUserIds);
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    for (Subscription subscription : subscriptions) {
      subscription.setStatus(SubscriptionStatus.ACTIVE);
      OffsetDateTime baseEndDate = subscription.getEndDate();
      if (baseEndDate == null || baseEndDate.isBefore(now)) {
        baseEndDate = now;
      }
      subscription.setEndDate(baseEndDate.plusMonths(1));
    }

    subscriptionRepository.saveAll(subscriptions);
  }

  private void expireOverdueSubscriptions(List<Subscription> subscriptions) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    for (Subscription subscription : subscriptions) {
      expireIfOverdue(subscription, now);
    }
  }

  private void expireIfOverdue(Subscription subscription, OffsetDateTime now) {
    if (subscription.getStatus() == SubscriptionStatus.ACTIVE
        && subscription.getEndDate() != null
        && subscription.getEndDate().isBefore(now)) {
      subscription.setStatus(SubscriptionStatus.EXPIRED);
      subscriptionRepository.save(subscription);
      log.info("Subscription with id: {} marked as EXPIRED", subscription.getId());
    }
  }
}
