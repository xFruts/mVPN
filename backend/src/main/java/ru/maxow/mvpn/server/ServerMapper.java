package ru.maxow.mvpn.server;

import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.maxow.mvpn.model.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.model.GetServerResponseDto;
import ru.maxow.mvpn.model.ListServerDto;
import ru.maxow.mvpn.model.ListServerNameAndLocationDto;
import ru.maxow.mvpn.model.ServerResponseDto;

@Mapper(componentModel = "spring")
public interface ServerMapper {
  ServerResponseDto toDto(Server server);

  GetServerResponseDto toGetDto(Server server);

  ListServerDto toListDto(Server servers);

  ListServerNameAndLocationDto toNameAndLocationDto(Server server);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "load", ignore = true)
  @Mapping(target = "usage", ignore = true)
  @Mapping(target = "ping", ignore = true)
  @Mapping(target = "uptime", ignore = true)
  @Mapping(target = "successfulChecks", ignore = true)
  @Mapping(target = "failedChecks", ignore = true)
  @Mapping(target = "hostKey", ignore = true)
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateFromDto(CreateUpdateServerRequestDto dto, @MappingTarget Server server);
}
