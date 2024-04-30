package task.system.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class UsernameValidation implements ConstraintValidator<Username, String> {
    private static final String PATTERN_OF_USERNAME = "^[a-zA-Z]+[a-zA-Z\\d]*$";

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        return username != null && Pattern.compile(PATTERN_OF_USERNAME).matcher(username).matches();
    }
}
