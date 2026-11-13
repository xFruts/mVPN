package ru.maxow.mvpn.xui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

/**
 * Сервис поиска и сборки конфигураций VLESS для клиентов 3X-UI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiVlessConfigService {

  private static final String PROTOCOL_VLESS = "vless";

  XuiInboundClient inboundClient;
  VlessLinkBuilder vlessLinkBuilder;

  String resolveVlessConfig(
      RestClient restClient,
      String sessionCookie,
      Server server,
      User user) {

    XuiInboundsResponse inbounds = inboundClient.getInbounds(restClient, sessionCookie);
    return findVlessConfigInInbounds(inbounds, user, server.getIp(), server.getCountryEmoji());
  }

  private String findVlessConfigInInbounds(
      XuiInboundsResponse inboundsResponse,
      User user,
      String serverIp,
      String countryEmoji) {

    List<XuiInboundsResponse.Inbound> inbounds = Optional.ofNullable(inboundsResponse)
        .map(XuiInboundsResponse::getValidInboundsOrThrow)
        .orElseThrow(() -> new XuiUnavailableException(
            "Failed to get inbounds or inbounds list is empty"));

    String expectedUuid = String.valueOf(user.getXuiId());

    // Поиск первого активного VLESS-инбаунда клиента и генерация ссылки конфигурации
    return inbounds.stream()
        .filter(inbound -> PROTOCOL_VLESS.equalsIgnoreCase(inbound.protocol()))
        .flatMap(inbound -> inbound.findClient(expectedUuid, user.getFullName())
            .stream()
            .map(clientNode -> Pair.of(inbound, clientNode)))
        .map(pair -> vlessLinkBuilder.generateVlessLink(
            pair.getSecond(), serverIp, countryEmoji, pair.getFirst()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new NotFoundException(
            String.format("VLESS config for user %s not found on server", user.getFullName())));
  }
}
