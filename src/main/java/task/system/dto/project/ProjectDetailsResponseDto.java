package task.system.dto.project;

import java.time.LocalDate;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import task.system.model.Project;

@Getter
@Setter
public class ProjectDetailsResponseDto {
    private Long id;
    private String name;
    private String description;
    private Set<Long> userIds;
    private LocalDate startDate;
    private LocalDate endDate;
    private Project.Status status;
}
