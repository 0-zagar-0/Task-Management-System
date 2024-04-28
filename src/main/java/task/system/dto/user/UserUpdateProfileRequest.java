package task.system.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import task.system.validation.FieldMatch;

@Getter
@Setter
@FieldMatch(field = "password",
        fieldMatch = "repeatPassword",
        message = "Password values don't match")
public class UserUpdateProfileRequest {
    @Email
    @Size(min = 8, max = 30)
    private String email;
    private String password;
    private String repeatPassword;
    @Size(min = 3, max = 30)
    private String username;
    @Size(min = 3, max = 30)
    private String firstName;
    @Size(min = 3, max = 30)
    private String lastName;
}
