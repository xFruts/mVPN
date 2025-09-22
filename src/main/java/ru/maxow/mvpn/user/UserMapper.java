package ru.maxow.mvpn.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
  UserResponseDto toUserResponseDto(User user);

  User toUser(UserRequestDto userRequestDto);

  @Mapping(target = "id", ignore = true)
  void updateUserFromUserResponseDto(UserRequestDto userRequestDto, @MappingTarget User user);
}
