package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XuiClientDtoFactory - Unit тесты")
class XuiClientDtoFactoryTest {

  private final XuiClientDtoFactory factory = new XuiClientDtoFactory();

  @Test
  @DisplayName("createClientDto: создаёт корректный DTO для нового клиента")
  void shouldCreateClientDtoForNewClient() {
    UUID xuiId = UUID.randomUUID();
    UUID subId = UUID.randomUUID();
    OffsetDateTime endDate = OffsetDateTime.now().plusMonths(1);

    User user = new User();
    user.setId(1L);
    user.setXuiId(xuiId);
    user.setFullName("test@mvpn.io");
    user.setXuiSubscription(subId);

    Tariff tariff = new Tariff();
    tariff.setTrafficLimitGb(100);
    tariff.setMaxDevices(5);

    Subscription subscription = new Subscription();
    subscription.setTariff(tariff);
    subscription.setEndDate(endDate);

    XuiClientDto dto = factory.createClientDto(user, subscription, tariff, null);

    assertThat(dto).isNotNull();
    assertThat(dto.uuid()).isEqualTo(xuiId.toString());
    assertThat(dto.email()).isEqualTo("test@mvpn.io");
    assertThat(dto.subId()).isEqualTo(subId.toString());
    assertThat(dto.totalGB()).isEqualTo(100L * 1024L * 1024L * 1024L);
    assertThat(dto.expiryTime()).isEqualTo(endDate.toInstant().toEpochMilli());
    assertThat(dto.enable()).isTrue();
    assertThat(dto.limitIp()).isEqualTo(5);
    assertThat(dto.flow()).isEqualTo("xtls-rprx-vision");
    assertThat(dto.password()).isNull();
    assertThat(dto.security()).isNull();
  }

  @Test
  @DisplayName("createClientDto: сохраняет существующий password и security при обновлении")
  void shouldPreservePasswordAndSecurityWhenClientExists() {
    User user = new User();
    user.setXuiId(UUID.randomUUID());
    user.setFullName("existing@mvpn.io");
    user.setXuiSubscription(UUID.randomUUID());

    Tariff tariff = new Tariff();
    tariff.setTrafficLimitGb(50);
    tariff.setMaxDevices(2);

    Subscription subscription = new Subscription();
    subscription.setTariff(tariff);
    subscription.setEndDate(OffsetDateTime.now().plusDays(10));

    XuiClientDto existing = new XuiClientDto(
        "old-id", "existing@mvpn.io", "sub", 0L, 0L, true, null, 1, "flow",
        "existing-pwd", "existing-security", null, null, null
    );

    XuiClientDto dto = factory.createClientDto(user, subscription, tariff, existing);

    assertThat(dto.password()).isEqualTo("existing-pwd");
    assertThat(dto.security()).isEqualTo("existing-security");
    assertThat(dto.email()).isEqualTo("existing@mvpn.io");
    assertThat(dto.totalGB()).isEqualTo(50L * 1024L * 1024L * 1024L);
  }
}
