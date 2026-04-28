package ru.maxow.mvpn.xui;

import java.util.UUID;

public interface ConfigFacade {
  SubscriptionConfigPayload getSubscriptionConfig(UUID verificationCode);
}
