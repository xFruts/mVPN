package ru.maxow.mvpn.tariff;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.util.Pair;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.tariff.dto.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.tariff.dto.TariffPlanResponseDto;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TariffPlanMapper {

  @Mapping(target = "servers", ignore = true)
  void updateFromDto(CreateUpdateRequestTariffPlanDto dto, @MappingTarget TariffPlan tariffPlan);

  @Mapping(target = "serverLocation", source = "servers")
  TariffPlanResponseDto toResponseDto(TariffPlan tariffPlan);

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
