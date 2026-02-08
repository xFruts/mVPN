package ru.maxow.mvpn.xui;

import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;

public interface XuiPanelService {
  String getVlessConfig(Server server, User user);
  void createClient(Server server, User user);
}
