package ru.maxow.mvpn.xui;

import org.springframework.stereotype.Component;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

/**
 * Фабрика построения DTO клиента для 3X-UI API.
 * <p>
 * Преобразует доменные сущности ({@link User}, {@link Subscription}, {@link Tariff})
 * в формат панели 3X-UI с учётом лимитов, срока действия и настроек безопасности.
 */
@Component
public class XuiClientDtoFactory {

  private static final String DEFAULT_FLOW = "xtls-rprx-vision";

  public XuiClientDto createClientDto(
      User user,
      Subscription subscription,
      Tariff tariff,
      XuiClientDto existing) {

    return new XuiClientDto(
        String.valueOf(user.getXuiId()),
        user.getFullName(),
        String.valueOf(user.getXuiSubscription()),
        tariff.getTrafficLimitBytes(),
        subscription.getEndDate().toInstant().toEpochMilli(),
        true,
        null,
        tariff.getMaxDevices(),
        DEFAULT_FLOW,
        existing != null ? existing.password() : null,
        existing != null ? existing.security() : null,
        null,
        null,
        null
    );
  }
}
