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
    @Mapping(target = "administrators", ignore = true)
    Project toEntity(ProjectRequestDto request);

    @Mapping(target = "userIds", source = "users", qualifiedByName = "setUserIds")
    @Mapping(target = "administratorIds",
            source = "administrators",
            qualifiedByName = "setAdministratorIds")
    @Mapping(target = "mainUser", source = "savedProject.mainUser.id")
    ProjectDetailsResponseDto toDto(Project savedProject);

    ProjectLowInfoResponse toLowInfoDto(Project project);

    @Named("setUserIds")
    default Set<Long> setUserIds(Set<User> users) {
        return users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }

    @Named("setAdministratorIds")
    default Set<Long> setAdministratorIds(Set<User> administrators) {
        return administrators.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }
}
