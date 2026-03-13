package ru.maxow.mvpn.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.PaymentVerificationRequestDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.payment.paymentverification.PaymentVerificationService;
import ru.maxow.mvpn.subscription.SubscriptionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentApprovalFacade - Unit тесты")
class PaymentApprovalFacadeTest {

  @Mock
  private PaymentVerificationService paymentVerificationService;

  @Mock
  private SubscriptionService subscriptionService;

  @InjectMocks
  private PaymentApprovalFacade paymentApprovalFacade;

  @Nested
  @DisplayName("approve")
  class ApproveTests {

    @Test
    @DisplayName("Должен одобрить verification и продлить подписку")
    void shouldApproveAndExtendSubscription() {
      PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
      request.setAdminComment("ok");

      PaymentVerificationResponseDto approved = new PaymentVerificationResponseDto();
      approved.setId(10L);
      approved.setUserId(7L);
      approved.setBillingMonth("2026-09");

      when(paymentVerificationService.approve(10L, "ok")).thenReturn(approved);

      PaymentVerificationResponseDto result = paymentApprovalFacade.approve(10L, request);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(10L);
      assertThat(result.getUserId()).isEqualTo(7L);

      verify(paymentVerificationService).approve(10L, "ok");
      verify(subscriptionService).extendSubscription(7L, "2026-09");
    }

    @Test
    @DisplayName("Не должен продлевать подписку, если approve завершился ошибкой")
    void shouldNotExtendWhenApproveFails() {
      PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
      request.setAdminComment("ok");

      when(paymentVerificationService.approve(10L, "ok"))
          .thenThrow(new RuntimeException("approve error"));

      assertThatThrownBy(() -> paymentApprovalFacade.approve(10L, request))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("approve error");

      verify(paymentVerificationService).approve(10L, "ok");
      verify(subscriptionService, never()).extendSubscription(anyLong(), anyString());
    }
  }

  @Nested
  @DisplayName("reject")
  class RejectTests {

    @Test
    @DisplayName("Должен вернуть результат reject и не трогать подписку")
    void shouldRejectWithoutExtendingSubscription() {
      PaymentVerificationRequestDto request = new PaymentVerificationRequestDto();
      request.setAdminComment("bad receipt");

      PaymentVerificationResponseDto rejected = new PaymentVerificationResponseDto();
      rejected.setId(11L);
      rejected.setUserId(8L);

      when(paymentVerificationService.reject(11L, "bad receipt")).thenReturn(rejected);

      PaymentVerificationResponseDto result = paymentApprovalFacade.reject(11L, request);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(11L);

      verify(paymentVerificationService).reject(11L, "bad receipt");
      verify(subscriptionService, never()).extendSubscription(anyLong(), anyString());
    }
  }
}

