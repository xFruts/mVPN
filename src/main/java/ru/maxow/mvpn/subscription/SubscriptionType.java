package ru.maxow.mvpn.subscription;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubscriptionType {
  TRIAL(7, Set.of(Protocol.XRAY)),
  BASIC(30, Set.of(Protocol.XRAY, Protocol.AMNEZIA_WG)),
  VIP(365, Set.of(Protocol.XRAY, Protocol.AMNEZIA_WG));

  private final int durationDays;
  private final Set<Protocol> availableProtocols;
}
