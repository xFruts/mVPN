package ru.maxow.mvpn.promocode;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.PromocodeResponseDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.user.UserService;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromocodeFacade - Unit тесты")
class PromocodeFacadeTest {

  @Mock
  private PromocodeService promocodeService;

  @Mock
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SubscriptionService subscriptionService;

  @InjectMocks
  private PromocodeFacadeImpl promocodeFacade;

  @Nested
  @DisplayName("Применение промокода")
  class ApplyPromocodeTests {

    @Test
    @DisplayName("Должен выбросить NotFoundException, если пользователь не существует")
    void shouldThrowNotFoundWhenUserDoesNotExist() {
      when(userRepository.existsById(77L)).thenReturn(false);

      assertThatThrownBy(() -> promocodeFacade.applyPromocode(77L, "PROMO1"))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getEntityName()).isEqualTo("User");
            assertThat(ex.getIdentifier()).isEqualTo(77L);
          });

      verify(userRepository).existsById(77L);
      verify(userService, never()).hasActiveSubscriptions(anyLong());
      verify(promocodeService, never()).usePromocode(anyString());
      verify(subscriptionService, never()).createSubscription(anyLong(), any());
    }

    @Test
    @DisplayName("Должен выбросить BadRequestException, если у пользователя есть активная подписка")
    void shouldThrowBadRequestWhenUserHasActiveSubscription() {
      when(userRepository.existsById(11L)).thenReturn(true);
      when(userService.hasActiveSubscriptions(11L)).thenReturn(true);

      assertThatThrownBy(() -> promocodeFacade.applyPromocode(11L, "PROMO1"))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("active subscriptions");

      verify(userRepository).existsById(11L);
      verify(userService).hasActiveSubscriptions(11L);
      verify(promocodeService, never()).usePromocode(anyString());
      verify(subscriptionService, never()).createSubscription(anyLong(), any());
    }

    @Test
    @DisplayName("Должен применить промокод и создать подписку")
    void shouldApplyPromocodeAndCreateSubscription() {
      PromocodeResponseDto promoDto = new PromocodeResponseDto();
      promoDto.setCode("PROMO1");

      SubscriptionResponseDto subscription = new SubscriptionResponseDto();
      subscription.setId(100L);
      subscription.setStatus(SubscriptionStatus.ACTIVE);

      when(userRepository.existsById(5L)).thenReturn(true);
      when(userService.hasActiveSubscriptions(5L)).thenReturn(false);
      when(promocodeService.usePromocode("PROMO1")).thenReturn(promoDto);
      when(subscriptionService.createSubscription(eq(5L), any(CreateUpdateSubscriptionDto.class)))
          .thenReturn(subscription);

      OffsetDateTime before = OffsetDateTime.now();
      promocodeFacade.applyPromocode(5L, "PROMO1");
      OffsetDateTime after = OffsetDateTime.now();

      verify(userRepository).existsById(5L);
      verify(userService).hasActiveSubscriptions(5L);
      verify(promocodeService).usePromocode("PROMO1");

      ArgumentCaptor<CreateUpdateSubscriptionDto> dtoCaptor =
          ArgumentCaptor.forClass(CreateUpdateSubscriptionDto.class);
      verify(subscriptionService).createSubscription(eq(5L), dtoCaptor.capture());

      CreateUpdateSubscriptionDto dto = dtoCaptor.getValue();
      assertThat(dto.getUserId()).isEqualTo(5L);
      assertThat(dto.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(dto.getStartDate()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
      assertThat(dto.getEndDate()).isAfter(dto.getStartDate().plusDays(27));
      assertThat(dto.getEndDate()).isBefore(dto.getStartDate().plusDays(33));
    }
  }
}

