package task.system.dto.project;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import task.system.model.Project;

@Getter
@Setter
public class ProjectUpdateRequestDto {
    @Size(min = 3, max = 50)
    private String name;

    @Size(min = 3)
    private String description;

    private Set<Long> administrators = new HashSet<>();

    private Set<Long> users = new HashSet<>();

    @FutureOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @FutureOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Project.Status status;
}
