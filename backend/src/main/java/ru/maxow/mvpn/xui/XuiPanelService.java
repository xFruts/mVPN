package ru.maxow.mvpn.xui;

import reactor.core.publisher.Mono;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;

public interface XuiPanelService {
  Mono<String> getVlessConfig(Server server, User user);
  Mono<Void> createClient(Server server, User user);
}
