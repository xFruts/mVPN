package ru.maxow.mvpn.subscription;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
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

  @InjectMocks
  private SubscriptionServiceImpl subscriptionService;

  private User testUser;
  private Subscription testSubscription;
  private SubscriptionResponseDto testSubscriptionDto;

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

    testSubscriptionDto = new SubscriptionResponseDto();
    testSubscriptionDto.setId(1L);
    testSubscriptionDto.setStatus(SubscriptionStatus.ACTIVE);
  }

  @Nested
  @DisplayName("Создание подписки (POST)")
  class CreateSubscriptionTests {

    @Test
    @DisplayName("Должен успешно создать подписку с валидными данными")
    void shouldCreateSubscriptionSuccessfully() {
      // Arrange
      Long userId = 1L;
      CreateUpdateSubscriptionDto requestDto = new CreateUpdateSubscriptionDto();
      OffsetDateTime startDate = OffsetDateTime.now();
      OffsetDateTime endDate = OffsetDateTime.now().plusMonths(1);
      requestDto.setStartDate(startDate);
      requestDto.setEndDate(endDate);
      requestDto.setStatus(SubscriptionStatus.ACTIVE);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);
      when(subscriptionMapper.toSubscriptionResponseDto(testSubscription)).thenReturn(testSubscriptionDto);

      // Act
      SubscriptionResponseDto result = subscriptionService.createSubscription(userId, requestDto);

      // Assert
      assertThat(result).isNotNull().isEqualTo(testSubscriptionDto);
      verify(userRepository).findById(userId);
      verify(subscriptionRepository).save(any(Subscription.class));
      verify(subscriptionMapper).toSubscriptionResponseDto(testSubscription);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException для несуществующего пользователя")
    void shouldThrowNotFoundExceptionForNonExistentUser() {
      // Arrange
      Long nonExistentUserId = 999L;
      CreateUpdateSubscriptionDto requestDto = new CreateUpdateSubscriptionDto();
      requestDto.setStartDate(OffsetDateTime.now());
      requestDto.setEndDate(OffsetDateTime.now().plusMonths(1));

      when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.createSubscription(nonExistentUserId, requestDto))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("User");

      verify(userRepository).findById(nonExistentUserId);
      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить IllegalArgumentException если дата окончания раньше даты начала")
    void shouldThrowIllegalArgumentExceptionWhenEndDateBeforeStartDate() {
      // Arrange
      Long userId = 1L;
      CreateUpdateSubscriptionDto requestDto = new CreateUpdateSubscriptionDto();
      OffsetDateTime startDate = OffsetDateTime.now();
      OffsetDateTime endDate = startDate.minusDays(1);
      requestDto.setStartDate(startDate);
      requestDto.setEndDate(endDate);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.createSubscription(userId, requestDto))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("End date cannot be before start date");

      verify(subscriptionRepository, never()).save(any());
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
      CreateUpdateSubscriptionDto updateDto = new CreateUpdateSubscriptionDto();
      OffsetDateTime newEndDate = OffsetDateTime.now().plusMonths(3);
      updateDto.setStartDate(OffsetDateTime.now());
      updateDto.setEndDate(newEndDate);
      updateDto.setStatus(SubscriptionStatus.CANCELED);

      Subscription updatedSubscription = new Subscription();
      updatedSubscription.setId(subscriptionId);
      updatedSubscription.setStatus(SubscriptionStatus.CANCELED);
      updatedSubscription.setEndDate(newEndDate);

      when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(testSubscription));
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
    @DisplayName("Должен успешно продлить подписку на один месяц")
    void shouldExtendSubscriptionSuccessfully() {
      // Arrange
      Long subscriptionId = 1L;
      String billingMonth = YearMonth.now().toString();

      when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      // Act
      subscriptionService.extendSubscription(subscriptionId, billingMonth);

      // Assert
      assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(testSubscription.getEndDate())
          .isAfter(OffsetDateTime.now().plusMonths(1).minusSeconds(60));
      verify(subscriptionRepository).findById(subscriptionId);
      verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при продлении несуществующей подписки")
    void shouldThrowNotFoundExceptionWhenExtendingNonExistentSubscription() {
      // Arrange
      Long nonExistentSubscriptionId = 999L;
      String billingMonth = YearMonth.now().toString();

      when(subscriptionRepository.findById(nonExistentSubscriptionId)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.extendSubscription(nonExistentSubscriptionId, billingMonth))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Subscription");

      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить IllegalArgumentException для некорректного формата даты")
    void shouldThrowIllegalArgumentExceptionForInvalidBillingMonthFormat() {
      // Arrange
      Long subscriptionId = 1L;
      String invalidBillingMonth = "invalid-date";

      when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(testSubscription));

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.extendSubscription(subscriptionId, invalidBillingMonth))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Invalid billing month format");

      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить IllegalArgumentException если месяц в прошлом")
    void shouldThrowIllegalArgumentExceptionWhenBillingMonthIsInPast() {
      // Arrange
      Long subscriptionId = 1L;
      String pastBillingMonth = YearMonth.now().minusMonths(1).toString();

      when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(testSubscription));

      // Act & Assert
      assertThatThrownBy(() -> subscriptionService.extendSubscription(subscriptionId, pastBillingMonth))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Billing month cannot be in the past");

      verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен переводить подписку в ACTIVE при продлении из CANCELED")
    void shouldSwitchStatusToActiveWhenExtendingCanceledSubscription() {
      Long subscriptionId = 1L;
      String billingMonth = YearMonth.now().toString();
      testSubscription.setStatus(SubscriptionStatus.CANCELED);

      when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      subscriptionService.extendSubscription(subscriptionId, billingMonth);

      assertThat(testSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      verify(subscriptionRepository).save(testSubscription);
    }

    @Test
    @DisplayName("Должен корректно продлять дату окончания на границе месяца")
    void shouldExtendEndDateOnMonthBoundary() {
      Long subscriptionId = 1L;
      String billingMonth = YearMonth.now().toString();
      OffsetDateTime boundaryDate = OffsetDateTime.parse("2026-01-31T00:00:00Z");
      testSubscription.setEndDate(boundaryDate);

      when(subscriptionRepository.findById(subscriptionId)).thenReturn(Optional.of(testSubscription));
      when(subscriptionRepository.save(any(Subscription.class))).thenReturn(testSubscription);

      subscriptionService.extendSubscription(subscriptionId, billingMonth);

      assertThat(testSubscription.getEndDate()).isEqualTo(boundaryDate.plusMonths(1));
      verify(subscriptionRepository).save(testSubscription);
    }
  }
}

