package ru.maxow.mvpn.subscription;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.maxow.mvpn.minio.MinioService;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.vpnconfig.AmneziaWgConfig;
import ru.maxow.mvpn.vpnconfig.VpnConfig;
import ru.maxow.mvpn.vpnconfig.VpnConfigMapper;
import ru.maxow.mvpn.vpnconfig.VpnConfigRepository;
import ru.maxow.mvpn.vpnconfig.VpnConfigResponseDto;
import ru.maxow.mvpn.vpnconfig.XrayConfig;

/**
 * Service implementation for managing subscriptions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionServiceImpl implements SubscriptionService {

  SubscriptionRepository subscriptionRepository;
  SubscriptionMapper subscriptionMapper;
  UserRepository userRepository;
  VpnConfigRepository vpnConfigRepository;
  VpnConfigMapper vpnConfigMapper;
  MinioService minioService;

  @Override
  @Transactional
  public SubscriptionResponseDto createSubscription(
      Long userId, SubscriptionRequestDto subscriptionRequestDto) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User", userId));
    SubscriptionType type = subscriptionRequestDto.type();

    if (type == SubscriptionType.TRIAL
        && subscriptionRepository.existsByUserAndType(user, SubscriptionType.TRIAL)) {
      throw new BadRequestException("Trial subscription already exists for this user.");
    }

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setType(type);
    subscription.setStartDate(LocalDateTime.now());
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    switch (type) {
      case TRIAL -> subscription.setEndDate(subscription.getStartDate().plusDays(7));
      case BASIC -> subscription.setEndDate(subscription.getStartDate().plusMonths(1));
      case VIP -> subscription.setEndDate(subscription.getStartDate().plusYears(1));
      default -> throw new BadRequestException("Unsupported subscription type: " + type);
    }

    Subscription savedSubscription = subscriptionRepository.save(subscription);
    log.info("{} subscription created for user with id: {}", type, userId);
    return subscriptionMapper.toSubscriptionResponseDto(savedSubscription);
  }

  @Override
  @Transactional
  public SubscriptionResponseDto updateSubscription(
      Long id, SubscriptionRequestDto subscriptionRequestDto) {
    Subscription subscription = findSubscriptionByIdInternal(id);
    subscriptionMapper.updateSubscriptionFromDto(subscriptionRequestDto, subscription);
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
  @Transactional
  public VpnConfigResponseDto addVpnConfigToSubscription(
      Long subscriptionId, Protocol protocol, MultipartFile file, String link) {
    Subscription subscription = findSubscriptionByIdInternal(subscriptionId);
    VpnConfig vpnConfig = createVpnConfig(protocol, file, link);
    vpnConfig.setSubscription(subscription);
    VpnConfig savedVpnConfig = vpnConfigRepository.save(vpnConfig);
    log.info("VpnConfig added for subscription with id: {}", subscriptionId);

    return vpnConfigMapper.toResponseDto(savedVpnConfig);
  }

  private VpnConfig createVpnConfig(Protocol protocol, MultipartFile file, String link) {
    switch (protocol) {
      case AMNEZIA_WG:
        if (file == null || file.isEmpty()) {
          throw new BadRequestException("File must be provided for AMNEZIA_WG protocol");
        }
        AmneziaWgConfig amneziaWgConfig = new AmneziaWgConfig();
        String filePath = minioService.uploadFile(file);
        amneziaWgConfig.setFilePath(filePath);
        return amneziaWgConfig;
      case XRAY:
        if (link == null || link.isBlank()) {
          throw new BadRequestException("Link must be provided for XRAY protocol");
        }
        XrayConfig xrayConfig = new XrayConfig();
        xrayConfig.setConnectionLink(link);
        return xrayConfig;
      default:
        throw new BadRequestException("Unsupported protocol: " + protocol);
    }
  }

  private Subscription findSubscriptionByIdInternal(Long id) {
    return subscriptionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Subscription", id));
  }
}
