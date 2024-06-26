package task.system.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PasswordValidation.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    String message() default "Password must contain at least one digit, one lowercase letter, "
            + "one uppercase letter, one special character, and have no spaces. "
            + "It must be at least 8 characters long.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
