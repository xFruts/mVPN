package ru.maxow.mvpn.server;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import ru.maxow.mvpn.model.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.model.ListServerDto;
import ru.maxow.mvpn.model.ServerResponseDto;

@Mapper(componentModel = "spring")
public interface ServerMapper {
  ServerResponseDto toDto(Server server);
  ListServerDto toListDto(Server servers);
  void updateFromDto(CreateUpdateServerRequestDto dto, @MappingTarget Server server);
}
