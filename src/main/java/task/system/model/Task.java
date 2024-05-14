package task.system.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import task.system.exception.DataProcessingException;

@Entity
@Getter
@Setter
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH;

        @JsonCreator
        public static Priority forValue(String value) {
            StringBuilder errorMessage = new StringBuilder();

            for (Priority priority : values()) {
                if (priority.name().equalsIgnoreCase(value)) {
                    return priority;
                }

                errorMessage.append(", ").append(priority.name());
            }

            throw new DataProcessingException("Invalid priority value, please enter valid value:"
                    + errorMessage.substring(1)
            );
        }
    }

    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED;

        @JsonCreator
        public static Status forValue(String value) {
            StringBuilder errorMessage = new StringBuilder();

            for (Status status : values()) {
                if (status.name().equalsIgnoreCase(value)) {
                    return status;
                }

                errorMessage.append(", ").append(status.name());
            }

            throw new DataProcessingException("Invalid status value, please enter valid value:"
                    + errorMessage.substring(1)
            );
        }
    }
}
