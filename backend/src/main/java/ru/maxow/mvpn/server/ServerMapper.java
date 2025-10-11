package ru.maxow.mvpn.server;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import ru.maxow.mvpn.server.dto.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.server.dto.ListServerDto;
import ru.maxow.mvpn.server.dto.ServerResponseDto;

@Mapper(componentModel = "spring")
public interface ServerMapper {
  ServerResponseDto toDto(Server server);
  ListServerDto toListDto(Server servers);
  Server updateFromDto(CreateUpdateServerRequestDto dto, @MappingTarget Server server);
}
