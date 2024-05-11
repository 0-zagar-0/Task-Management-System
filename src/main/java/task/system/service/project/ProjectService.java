package task.system.service.project;

import java.util.List;
import task.system.dto.project.ProjectDetailsResponseDto;
import task.system.dto.project.ProjectLowInfoResponse;
import task.system.dto.project.ProjectRequestDto;
import task.system.dto.project.ProjectUpdateRequestDto;

public interface ProjectService {
    ProjectDetailsResponseDto create(ProjectRequestDto request);

    List<ProjectLowInfoResponse> getAllUserProjects();

    ProjectDetailsResponseDto getById(Long id);

    ProjectDetailsResponseDto updateById(Long id, ProjectUpdateRequestDto request);

    void deleteById(Long id);
}
