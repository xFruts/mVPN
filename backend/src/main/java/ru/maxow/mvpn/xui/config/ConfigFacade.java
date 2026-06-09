package ru.maxow.mvpn.xui.config;

import ru.maxow.mvpn.xui.dto.SubscriptionConfigPayload;
import java.util.UUID;

public interface ConfigFacade {
  SubscriptionConfigPayload getSubscriptionConfig(UUID verificationCode);
}
