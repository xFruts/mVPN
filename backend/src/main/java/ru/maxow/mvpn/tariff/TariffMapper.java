package ru.maxow.mvpn.tariff;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.util.Pair;
import ru.maxow.mvpn.model.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.model.TariffPlanResponseDto;
import ru.maxow.mvpn.server.Server;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TariffMapper {

  @Mapping(target = "servers", ignore = true)
  void updateFromDto(CreateUpdateRequestTariffPlanDto dto, @MappingTarget Tariff tariff);

  @Mapping(target = "serverLocation", source = "servers")
  TariffPlanResponseDto toResponseDto(Tariff tariff);

  default List<Pair<Long, String>> mapServersToPairs(List<Server> servers) {
    if (servers == null) {
      return new ArrayList<>();
    }
    return servers.stream()
        .map(this::serverToPair)
        .toList();
  }

  default Pair<Long, String> serverToPair(Server server) {
    return Pair.of(server.getId(), server.getLocation());
  }
}
