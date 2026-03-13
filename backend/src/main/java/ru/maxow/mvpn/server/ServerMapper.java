package ru.maxow.mvpn.server;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.maxow.mvpn.model.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.model.ListServerDto;
import ru.maxow.mvpn.model.ServerResponseDto;

@Mapper(componentModel = "spring")
public interface ServerMapper {
  ServerResponseDto toDto(Server server);

  ListServerDto toListDto(Server servers);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "load", ignore = true)
  @Mapping(target = "usage", ignore = true)
  @Mapping(target = "ping", ignore = true)
  @Mapping(target = "uptime", ignore = true)
  @Mapping(target = "successfulChecks", ignore = true)
  @Mapping(target = "failedChecks", ignore = true)
  @Mapping(target = "hostKey", ignore = true)
  void updateFromDto(CreateUpdateServerRequestDto dto, @MappingTarget Server server);
}
