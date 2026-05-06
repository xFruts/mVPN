package ru.maxow.mvpn.xui.config;

import ru.maxow.mvpn.xui.SubscriptionConfigPayload;
import java.util.UUID;

public interface ConfigFacade {
  SubscriptionConfigPayload getSubscriptionConfig(UUID verificationCode);
}
