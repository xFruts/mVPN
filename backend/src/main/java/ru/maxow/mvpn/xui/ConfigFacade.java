package ru.maxow.mvpn.xui;

import java.util.UUID;

public interface ConfigFacade {
  String getSubscriptionConfig(UUID verificationCode);
}
