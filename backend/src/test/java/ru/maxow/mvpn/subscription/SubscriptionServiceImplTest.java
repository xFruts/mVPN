package ru.maxow.mvpn.subscription;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficStateService;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionServiceImpl - Unit тесты (бизнес-логика)")
class SubscriptionServiceImplTest {

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private SubscriptionMapper subscriptionMapper;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TariffRepository tariffRepository;

  @Mock
  private SubscriptionTrafficStateService trafficStateService;

  @InjectMocks
  private SubscriptionServiceImpl subscriptionService;

  private User testUser;
  private Subscription testSubscription;
  private SubscriptionResponseDto testSubscriptionDto;

  private String futureBillingDate;
  private String pastBillingDate;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setFullName("John Doe");

    testSubscription = new Subscription();
    testSubscription.setId(1L);
    testSubscription.setUser(testUser);
    testSubscription.setStartDate(OffsetDateTime.now());
    testSubscription.setEndDate(OffsetDateTime.now().plusMonths(1));
    testSubscription.setStatus(SubscriptionStatus.ACTIVE);

    Tariff testTariff = new Tariff();
    testTariff.setId(1L);
    testTariff.setTrafficLimitGb(100);
    testTariff.setDurationOfDays(30);
    testSubscription.setTariff(testTariff);

    testSubscriptionDto = new SubscriptionResponseDto();
    testSubscriptionDto.setId(1L);
    testSubscriptionDto.setStatus(SubscriptionStatus.ACTIVE);

    futureBillingDate = java.time.LocalDate.now().plusDays(30)
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
    pastBillingDate = java.time.LocalDate.now().minusDays(1)
        .format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  @Nested
  @DisplayName("Информация о подписке по verification code")
  class SubscriptionInfoTests {

    @Test
    @DisplayName("Должен вернуть информацию о подписке со статистикой трафика")
    void shouldReturnSubscriptionInfoWithTrafficForVerificationCode() {
      UUID code = UUID.randomUUID();
      OffsetDateTime endDate = OffsetDateTime.of(2026, 4, 30, 0, 0, 0, 0, ZoneOffset.UTC);
      testSubscription.setEndDate(endDate);

      // Mock traffic state
      SubscriptionTrafficState trafficState = new SubscriptionTrafficState();
      trafficState.setUsedUploadBytes(1000L);
      trafficState.setUsedDownloadBytes(2000L);

      when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(testUser.getId()))
          .thenReturn(Optional.of(testSubscription));
      when(trafficStateService.syncTrafficForSubscription(testUser, testSubscription))
          .thenReturn(trafficState);

      String result = subscriptionService.getSubscriptionInfoForUserByCode(code);

      long trafficLimitBytes = 100L * 1024L * 1024L * 1024L;
      long expireSec = endDate.toInstant().getEpochSecond();
      String expected = String.format(
          "upload=%d; download=%d; total=%d; expire=%d",
          1000L, 2000L, trafficLimitBytes, expireSec
      );
      
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Должен использовать кэшированный traffic-state, если он уже есть")
    void shouldReturnSubscriptionInfoFromCachedTrafficStateWithoutResync() {
      UUID code = UUID.randomUUID();
      testSubscription.setEndDate(OffsetDateTime.of(2026, 4, 30, 0, 0, 0, 0, ZoneOffset.UTC));

      SubscriptionTrafficState trafficState = new SubscriptionTrafficState();
      trafficState.setUsedUploadBytes(111L);
      trafficState.setUsedDownloadBytes(222L);

      when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(testUser.getId()))
          .thenReturn(Optional.of(testSubscription));
      when(trafficStateService.getTrafficStateBySubscriptionId(testSubscription.getId()))
          .thenReturn(Optional.of(trafficState));

      String result = subscriptionService.getSubscriptionInfoForUserByCode(code);

      assertThat(result).contains("upload=111");
      assertThat(result).contains("download=222");
      verify(trafficStateService, never()).syncTrafficForSubscription(any(), any());
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException если пользователь не найден по verification code")
    void shouldThrowNotFoundWhenUserMissingByVerificationCode() {
      UUID code = UUID.randomUUID();
      when(userRepository.findByVerificationCode(code)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> subscriptionService.getSubscriptionInfoForUserByCode(code))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("User by verification code");
    }
  }

  @Nested
  @DisplayName("Создание подписки (POST)")
  class CreateSubscriptionTests {

    @Test
    @DisplayName("Должен успешно создать подписку с валидными данными")
    void shouldCreateSubscriptionSuccessfully() {
      // Arrange
      Long userId = 1L;
      Long tariffId = 1L;
      CreateUpdateSubscriptionDto requestDto = new CreateUpdateSubscriptionDto();
      OffsetDateTime startDate = OffsetDateTime.now();
      requestDto.setStartDate(startDate);
      requestDto.setStatus(SubscriptionStatus.ACTIVE);
      requestDto.setTariffId(tariffId);

      Tariff tariff = new Tariff();
      tariff.setDurationOfDays(30);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
      when(subscriptionMapper.toSubscriptionResponseDto(testSubscription)).thenReturn(testSubscriptionDto);

      // Act
      SubscriptionResponseDto result = subscriptionService.createSubscription(userId, requestDto);

      // Assert
      assertThat(result).isNotNull().isEqualTo(testSubscriptionDto);
      verify(userRepository).findById(userId);
      verify(tariffRepository).findById(tariffId);
      verify(subscriptionRepository).save(any(Subscription.class));
      verify(subscriptionMapper).toSubscriptionResponseDto(testSubscription);

      ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
      verify(subscriptionRepository).save(captor.capture());
      assertThat(captor.getValue().getStartDate()).isEqualTo(startDate);
      assertThat(captor.getValue().getEndDate()).isEqualTo(startDate.plusDays(30));
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException для несуществующего пользователя")
    void shouldThrowNotFoundExceptionForNonExistentUser() {
      // Arrange
      Long nonExistentUserId = 999L;
      CreateUpdateSubscriptionDto requestDto = new CreateUpdateSubscriptionDto();
      requestDto.setStartDate(OffsetDateTime.now());

      when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.createSubscription(nonExistentUserId, requestDto))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("User");

      verify(userRepository).findById(nonExistentUserId);
      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен использовать текущую дату UTC как startDate, если она не передана")
    void shouldUseCurrentUtcStartDateWhenStartDateIsMissing() {
      Long userId = 1L;
      Long tariffId = 1L;
      CreateUpdateSubscriptionDto requestDto = new CreateUpdateSubscriptionDto();
      requestDto.setStatus(SubscriptionStatus.ACTIVE);
      requestDto.setTariffId(tariffId);

      Tariff tariff = new Tariff();
      tariff.setDurationOfDays(10);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
      when(subscriptionMapper.toSubscriptionResponseDto(testSubscription)).thenReturn(testSubscriptionDto);

      OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
      subscriptionService.createSubscription(userId, requestDto);
      OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

      ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
      verify(subscriptionRepository).save(captor.capture());

      assertThat(captor.getValue().getStartDate()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
      assertThat(captor.getValue().getEndDate()).isEqualTo(captor.getValue().getStartDate().plusDays(10));
    }
  }

  @Nested
  @DisplayName("Обновление подписки (PUT)")
  class UpdateSubscriptionTests {

    @Test
    @DisplayName("Должен успешно обновить подписку")
    void shouldUpdateSubscriptionSuccessfully() {
      // Arrange
      Long subscriptionId = 1L;
      Long tariffId = 1L;
      CreateUpdateSubscriptionDto updateDto = new CreateUpdateSubscriptionDto();
      OffsetDateTime newEndDate = OffsetDateTime.now().plusMonths(3);
      updateDto.setStartDate(OffsetDateTime.now());
      updateDto.setStatus(SubscriptionStatus.CANCELED);
      updateDto.setTariffId(tariffId);

      Subscription updatedSubscription = new Subscription();
      updatedSubscription.setId(subscriptionId);
      updatedSubscription.setStatus(SubscriptionStatus.CANCELED);
      updatedSubscription.setEndDate(newEndDate);

      when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(testSubscription));
      Tariff tariff = new Tariff();
      tariff.setDurationOfDays(30);
      when(tariffRepository.findById(tariffId)).thenReturn(Optional.of(tariff));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(updatedSubscription);
      when(subscriptionMapper.toSubscriptionResponseDto(updatedSubscription)).thenReturn(testSubscriptionDto);
      doNothing().when(subscriptionMapper).updateSubscriptionFromDto(updateDto, testSubscription);

      // Act
      SubscriptionResponseDto result = subscriptionService.updateSubscription(subscriptionId, updateDto);

      // Assert
      assertThat(result).isNotNull();
      verify(subscriptionRepository).findById(subscriptionId);
      verify(subscriptionRepository).save(any(Subscription.class));
      verify(subscriptionMapper).updateSubscriptionFromDto(updateDto, testSubscription);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException для несуществующей подписки при обновлении")
    void shouldThrowNotFoundExceptionWhenUpdatingNonExistentSubscription() {
      // Arrange
      Long nonExistentSubscriptionId = 999L;
      CreateUpdateSubscriptionDto updateDto = new CreateUpdateSubscriptionDto();

      when(subscriptionRepository.findById(nonExistentSubscriptionId)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.updateSubscription(nonExistentSubscriptionId, updateDto))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Subscription");

      verify(subscriptionRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Удаление подписки (DELETE)")
  class DeleteSubscriptionTests {

    @Test
    @DisplayName("Должен успешно удалить подписку")
    void shouldDeleteSubscriptionSuccessfully() {
      // Arrange
      Long subscriptionId = 1L;
      when(subscriptionRepository.existsById(subscriptionId)).thenReturn(true);
      doNothing().when(subscriptionRepository).deleteById(subscriptionId);

      // Act
      subscriptionService.deleteSubscription(subscriptionId);

      // Assert
      verify(subscriptionRepository).existsById(subscriptionId);
      verify(subscriptionRepository).deleteById(subscriptionId);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при удалении несуществующей подписки")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistentSubscription() {
      // Arrange
      Long nonExistentSubscriptionId = 999L;
      when(subscriptionRepository.existsById(nonExistentSubscriptionId)).thenReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.deleteSubscription(nonExistentSubscriptionId))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Subscription");

      verify(subscriptionRepository, never()).deleteById(anyLong());
    }
  }

  @Nested
  @DisplayName("Получение подписок (GET)")
  class GetSubscriptionTests {

    @Test
    @DisplayName("Должен успешно получить подписку по id")
    void shouldGetSubscriptionByIdSuccessfully() {
      // Arrange
      Long subscriptionId = 1L;
      when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(testSubscription));
      when(subscriptionMapper.toSubscriptionResponseDto(testSubscription)).thenReturn(testSubscriptionDto);

      // Act
      SubscriptionResponseDto result = subscriptionService.findSubscriptionById(subscriptionId);

      // Assert
      assertThat(result).isNotNull().isEqualTo(testSubscriptionDto);
      verify(subscriptionRepository).findById(subscriptionId);
      verify(subscriptionMapper).toSubscriptionResponseDto(testSubscription);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при получении несуществующей подписки")
    void shouldThrowNotFoundExceptionWhenGettingNonExistentSubscription() {
      // Arrange
      Long nonExistentSubscriptionId = 999L;
      when(subscriptionRepository.findById(nonExistentSubscriptionId)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.findSubscriptionById(nonExistentSubscriptionId))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Subscription");

      verify(subscriptionRepository).findById(nonExistentSubscriptionId);
      verify(subscriptionMapper, never()).toSubscriptionResponseDto(any());
    }

    @Test
    @DisplayName("Должен получить все подписки пользователя")
    void shouldGetAllSubscriptionsByUserId() {
      // Arrange
      Long userId = 1L;
      Subscription subscription2 = new Subscription();
      subscription2.setId(2L);
      List<Subscription> subscriptions = List.of(testSubscription, subscription2);

      SubscriptionResponseDto dto2 = new SubscriptionResponseDto();
      dto2.setId(2L);

      when(subscriptionRepository.findByUser_Id(userId)).thenReturn(subscriptions);
      when(subscriptionMapper.toSubscriptionResponseDto(testSubscription)).thenReturn(testSubscriptionDto);
      when(subscriptionMapper.toSubscriptionResponseDto(subscription2)).thenReturn(dto2);

      // Act
      List<SubscriptionResponseDto> result = subscriptionService.findSubscriptionsByUserId(userId);

      // Assert
      assertThat(result).hasSize(2).containsExactly(testSubscriptionDto, dto2);
      verify(subscriptionRepository).findByUser_Id(userId);
      verify(subscriptionMapper, times(2)).toSubscriptionResponseDto(any(Subscription.class));
    }

    @Test
    @DisplayName("Должен вернуть пустой список если у пользователя нет подписок")
    void shouldReturnEmptyListWhenUserHasNoSubscriptions() {
      // Arrange
      Long userId = 1L;
      when(subscriptionRepository.findByUser_Id(userId)).thenReturn(List.of());

      // Act
      List<SubscriptionResponseDto> result = subscriptionService.findSubscriptionsByUserId(userId);

      // Assert
      assertThat(result).isEmpty();
      verify(subscriptionRepository).findByUser_Id(userId);
      verify(subscriptionMapper, never()).toSubscriptionResponseDto(any());
    }
  }

  @Nested
  @DisplayName("Продление подписки")
  class ExtendSubscriptionTests {

    @Test
    @DisplayName("Должен успешно продлить подписку для текущего месяца")
    void shouldExtendSubscriptionSuccessfully() {
      // Arrange
      Long userId = 1L;
      String billingDate = futureBillingDate;
      OffsetDateTime originalEndDate = OffsetDateTime.now().minusDays(10);
      testSubscription.setEndDate(originalEndDate);
      testSubscription.setStatus(SubscriptionStatus.ACTIVE);

      OffsetDateTime expectedEndDate = java.time.LocalDate.parse(billingDate)
          .atStartOfDay()
          .atOffset(ZoneOffset.UTC);

      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      subscriptionService.extendSubscription(userId, billingDate);

      // Assert
      assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(testSubscription.getEndDate()).isEqualTo(expectedEndDate);
      verify(subscriptionRepository).findFirstByUser_IdOrderByStartDateDesc(userId);
      verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("Должен выбросить BadRequestException если у пользователя нет подписок")
    void shouldThrowBadRequestExceptionWhenUserHasNoSubscriptions() {
      // Arrange
      Long userId = 1L;
      String billingDate = futureBillingDate;

      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.extendSubscription(userId, billingDate))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("User has no active subscriptions");

      verify(subscriptionRepository).findFirstByUser_IdOrderByStartDateDesc(userId);
      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить BadRequestException для некорректного формата billingMonth")
    void shouldThrowBadRequestExceptionForInvalidBillingMonthFormat() {
      // Arrange
      Long userId = 1L;
      String invalidBillingMonth = "invalid-date";

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.extendSubscription(userId, invalidBillingMonth))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Invalid billing date format");

      verify(subscriptionRepository, never()).findFirstByUser_IdOrderByStartDateDesc(anyLong());
      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить BadRequestException если billingMonth в прошлом")
    void shouldThrowBadRequestExceptionWhenBillingMonthIsInPast() {
      // Arrange
      Long userId = 1L;
      String pastBillingMonth = pastBillingDate;

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.extendSubscription(userId, pastBillingMonth))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Billing month cannot be in the past");

      verify(subscriptionRepository, never()).findFirstByUser_IdOrderByStartDateDesc(anyLong());
      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен переводить подписку в ACTIVE при продлении из CANCELED")
    void shouldSwitchStatusToActiveWhenExtendingCanceledSubscription() {
      // Arrange
      Long userId = 1L;
      String billingMonth = futureBillingDate;
      testSubscription.setStatus(SubscriptionStatus.CANCELED);
      testSubscription.setEndDate(OffsetDateTime.now().plusMonths(1));

      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      subscriptionService.extendSubscription(userId, billingMonth);

      // Assert
      assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("Должен корректно продлять дату окончания на границе месяца")
    void shouldExtendEndDateOnMonthBoundary() {
      // Arrange
      Long userId = 1L;
      String billingMonth = java.time.LocalDate.now().plusDays(10)
          .format(DateTimeFormatter.ISO_LOCAL_DATE);
      OffsetDateTime boundaryDate = OffsetDateTime.parse("2026-01-31T00:00:00Z");
      testSubscription.setEndDate(boundaryDate);
      testSubscription.setStatus(SubscriptionStatus.ACTIVE);

      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      subscriptionService.extendSubscription(userId, billingMonth);

      // Assert
      assertThat(testSubscription.getEndDate()).isEqualTo(
          java.time.LocalDate.parse(billingMonth).atStartOfDay().atOffset(ZoneOffset.UTC));
      verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("Должен продлить истекшую и отмененную подписку по текущей логике")
    void shouldExtendSubscriptionWhenSubscriptionIsExpiredAndCanceled() {
      // Arrange
      Long userId = 1L;
      String billingMonth = futureBillingDate;
      testSubscription.setStatus(SubscriptionStatus.CANCELED);
      testSubscription.setEndDate(OffsetDateTime.now().minusMonths(1)); // истекла месяц назад

      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      subscriptionService.extendSubscription(userId, billingMonth);

      // Assert
      assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(testSubscription.getEndDate()).isEqualTo(
          java.time.LocalDate.parse(billingMonth).atStartOfDay().atOffset(ZoneOffset.UTC));
      verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("Должен успешно продлить подписку для будущего месяца")
    void shouldExtendSubscriptionForFutureMonth() {
      // Arrange
      Long userId = 1L;
      String futureBillingMonth = java.time.LocalDate.now().plusDays(45)
          .format(DateTimeFormatter.ISO_LOCAL_DATE);
      testSubscription.setStatus(SubscriptionStatus.ACTIVE);
      testSubscription.setEndDate(OffsetDateTime.now().plusMonths(1));

      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      subscriptionService.extendSubscription(userId, futureBillingMonth);

      // Assert
      assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(testSubscription.getEndDate()).isEqualTo(
          java.time.LocalDate.parse(futureBillingMonth).atStartOfDay().atOffset(ZoneOffset.UTC));
      verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("Должен сохранить текущую endDate если она позже paidUntilDate")
    void shouldKeepCurrentEndDateWhenItIsAfterPaidUntilDate() {
      // Arrange
      Long userId = 1L;
      String billingMonth = java.time.LocalDate.now().plusDays(5)
          .format(DateTimeFormatter.ISO_LOCAL_DATE);
      OffsetDateTime currentEndDate = OffsetDateTime.now().plusDays(20).withNano(0);
      testSubscription.setEndDate(currentEndDate);
      testSubscription.setStatus(SubscriptionStatus.ACTIVE);

      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      subscriptionService.extendSubscription(userId, billingMonth);

      // Assert
      assertThat(testSubscription.getEndDate()).isEqualTo(currentEndDate);
      verify(subscriptionRepository).save(testSubscription);
    }
  }

  @Nested
  @DisplayName("Продление подписки на 1 месяц (bulk)")
  class ExtendSubscriptionsByUserIdsTests {

    @Test
    @DisplayName("Должен продлить endDate на месяц для всех пользователей")
    void shouldExtendSubscriptionsForUsers() {
      Long userId = 1L;
      OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
      testSubscription.setEndDate(endDate);

      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
      when(subscriptionMapper.toSubscriptionResponseDto(testSubscription)).thenReturn(testSubscriptionDto);

      List<SubscriptionResponseDto> result =
          subscriptionService.extendSubscriptionsByUserIds(List.of(userId));

      assertThat(result).containsExactly(testSubscriptionDto);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<Subscription>> captor = ArgumentCaptor.forClass(List.class);
      verify(subscriptionRepository).saveAll(captor.capture());

      Subscription saved = captor.getValue().getFirst();
      assertThat(saved.getEndDate()).isEqualTo(endDate.plusMonths(1));
      assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Должен выбросить BadRequestException если список пользователей пустой")
    void shouldThrowWhenUserIdsEmpty() {
      assertThatThrownBy(() -> subscriptionService.extendSubscriptionsByUserIds(List.of()))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("User IDs list cannot be empty");

      verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("Должен выбросить BadRequestException если подписка не найдена")
    void shouldThrowWhenSubscriptionMissing() {
      Long userId = 2L;
      when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(userId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> subscriptionService.extendSubscriptionsByUserIds(List.of(userId)))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("No subscription found");

      verify(subscriptionRepository, never()).saveAll(any());
    }
  }
}
