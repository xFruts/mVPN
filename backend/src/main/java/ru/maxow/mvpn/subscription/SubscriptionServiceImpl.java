package ru.maxow.mvpn.subscription;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    Tariff tariff = tariffRepository.findById(dto.getTariffId())
        .orElseThrow(() -> new NotFoundException("Tariff", dto.getTariffId()));
    subscription.setTariff(tariff);

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
  public String getSubscriptionInfoForUserByCode(UUID verificationCode) {
    User user = userRepository.findByVerificationCode(verificationCode)
        .orElseThrow(() -> new NotFoundException("User by verification code"));

    Subscription subscription = subscriptionRepository
        .findFirstByUser_IdOrderByStartDateDesc(user.getId())
        .orElseThrow(() -> new NotFoundException("Subscription for user", user.getId()));

    return String.format("expire=%s", subscription.getEndDate().toInstant().getEpochSecond());
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
}
