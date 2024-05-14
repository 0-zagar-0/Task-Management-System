package task.system.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import task.system.config.MapperConfig;
import task.system.dto.task.TaskCreateRequestDto;
import task.system.dto.task.TaskFullDetailsDto;
import task.system.dto.task.TaskLowDetailsDto;
import task.system.model.Task;

@Mapper(config = MapperConfig.class)
public interface TaskMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Task toEntity(TaskCreateRequestDto request);

    TaskFullDetailsDto toFullDetailsDto(Task task);

    TaskLowDetailsDto toLowDetailsDto(Task task);
}
