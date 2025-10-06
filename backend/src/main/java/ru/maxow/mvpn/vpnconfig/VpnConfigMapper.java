package ru.maxow.mvpn.vpnconfig;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VpnConfigMapper {

  VpnConfigResponseDto toResponseDto(VpnConfig vpnConfig);
}
