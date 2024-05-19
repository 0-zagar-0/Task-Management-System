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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import task.system.exception.DataProcessingException;

@Entity
@Getter
@Setter
@ToString
@Table(name = "labels")
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private Color color;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    public enum Color {
        RED("#FF0000"),
        GREEN("#00FF00"),
        BLUE("#0000FF"),
        YELLOW("#FFFF00"),
        ORANGE("#FFA500"),
        PURPLE("#800080"),
        PINK("#FFC0CB"),
        BROWN("#A52A2A"),
        BLACK("#000000"),
        WHITE("#FFFFFF"),
        GRAY("#808080"),
        CYAN("#00FFFF"),
        MAGENTA("#FF00FF"),
        LIME("#00FF00"),
        NAVY("#000080");

        private final String hexCode;

        Color(String hexCode) {
            this.hexCode = hexCode;
        }

        public String getHexCode() {
            return hexCode;
        }

        @JsonCreator
        public static Color forValue(String value) {
            StringBuilder errorMessage = new StringBuilder();

            for (Color color : values()) {
                if (color.name().equalsIgnoreCase(value)) {
                    return color;
                }
                errorMessage.append(", ").append(color.name());
            }
            throw new DataProcessingException("Invalid color value, please enter valid value:"
                    + errorMessage.substring(1)
            );
        }
    }
}
