package ru.maxow.mvpn.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**  Mapper for converting between User entity and DTOs.*/
@Mapper(componentModel = "spring")
public interface UserMapper {
  /** Converts a User entity to a UserResponseDto. */
  UserResponseDto toUserResponseDto(User user);

  /** Converts a UserRequestDto to a User entity. */
  User toUser(UserRequestDto userRequestDto);

  /** Updates an existing User entity with data from a UserRequestDto, ignoring the ID field. */
  @Mapping(target = "id", ignore = true)
  void updateUserFromUserResponseDto(UserRequestDto userRequestDto, @MappingTarget User user);
}
