package ru.maxow.mvpn.xui;

import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ConfigFacade {
  Mono<String> getSubscriptionConfig(UUID verificationCode);
}
