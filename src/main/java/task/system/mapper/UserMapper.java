package task.system.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import task.system.config.MapperConfig;
import task.system.dto.user.UserRegisterRequestDto;
import task.system.dto.user.UserResponseDto;
import task.system.model.User;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    UserResponseDto toDto(User user);

    @Mapping(target = "role", expression = "java(User.Role.ROLE_USER)")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    User toEntity(UserRegisterRequestDto request);
}
