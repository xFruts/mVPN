package ru.maxow.mvpn.xui;

import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.user.User;

/**
 * Фасад интеграции с 3X-UI Panel.
 * <p>
 * Предоставляет операции для управления VPN-клиентами на панели:
 * создание/обновление клиентов, генерация конфигов и получение трафика.
 */
public interface XuiPanelService {

  /**
   * Генерирует VLESS-ссылку для пользователя на указанном сервере.
   * При необходимости создаёт/обновляет клиента на панели.
   */
  String getVlessConfig(Server server, User user);

  /**
   * Получает JSON-конфиг подписки для пользователя на указанном сервере.
   * При необходимости создаёт/обновляет клиента на панели.
   */
  String getJsonConfig(Server server, User user);

  /**
   * Создаёт или обновляет клиента на панели 3X-UI.
   * Subscription передаётся извне, чтобы избежать обращения к БД из инфраструктурного слоя.
   *
   * @param server       целевой сервер
   * @param user         пользователь
   * @param subscription активная подписка (содержит тариф с лимитами)
   */
  void createOrUpdateClient(Server server, User user, Subscription subscription);

  /**
   * Возвращает трафик клиента по email.
   *
   * @param server      целевой сервер
   * @param clientEmail email клиента (соответствует {@code user.getFullName()})
   */
  XuiClientTraffic getClientTraffic(Server server, String clientEmail);
}
