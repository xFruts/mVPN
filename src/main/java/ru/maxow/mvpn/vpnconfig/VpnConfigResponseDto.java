package ru.maxow.mvpn.vpnconfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.maxow.mvpn.subscription.Protocol;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VpnConfigResponseDto (
    Long id,
    Protocol protocol,
    String filePath, // For file-based configs like Amnezia
    String connectionLink // For link-based configs like X-Ray
) {
}
