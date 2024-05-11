package task.system.mapper;

import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import task.system.config.MapperConfig;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.project.ProjectLowInfoResponse;
import task.system.dto.project.ProjectRequestDto;
import task.system.model.Project;
import task.system.model.User;

@Mapper(config = MapperConfig.class)
public interface ProjectMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "mainUser", ignore = true)
    Project toEntity(ProjectRequestDto request);

    @Mapping(target = "userIds", source = "users", qualifiedByName = "setUserIds")
    ProjectDetailsResponseDto toDto(Project savedProject);

    ProjectLowInfoResponse toLowInfoDto(Project project);

    @Named("setUserIds")
    default Set<Long> setUserIds(Set<User> users) {
        return users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }
}
