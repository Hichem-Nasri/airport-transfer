package com.airporttransfer.user;

import com.airporttransfer.user.UserDtos.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);
}
