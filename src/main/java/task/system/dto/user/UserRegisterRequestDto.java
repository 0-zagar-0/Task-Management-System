package task.system.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import task.system.validation.FieldMatch;
import task.system.validation.Password;
import task.system.validation.Username;

@Getter
@Setter
@FieldMatch(field = "password",
        fieldMatch = "repeatPassword",
        message = "Password values don't match")
public class UserRegisterRequestDto {
    @Email
    @NotBlank
    @Size(min = 8, max = 30)
    private String email;
    @NotBlank
    @Password
    private String password;
    @NotBlank
    @Password
    private String repeatPassword;
    @NotBlank
    @Size(min = 3, max = 30)
    @Username
    private String username;
    @NotBlank
    @Size(min = 3, max = 30)
    private String firstName;
    @NotBlank
    @Size(min = 3, max = 30)
    private String lastName;
}
