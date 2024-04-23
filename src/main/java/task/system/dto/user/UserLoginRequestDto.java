package task.system.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import task.system.validation.Password;

@Getter
@Setter
public class UserLoginRequestDto {
    @NotBlank
    @Email
    @Size(min = 3, max = 30)
    private String email;
    @NotBlank
    @Password
    private String password;
}
