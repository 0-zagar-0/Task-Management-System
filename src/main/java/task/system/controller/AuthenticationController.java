package task.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.user.UserLoginRequestDto;
import task.system.dto.user.UserLoginResponseDto;
import task.system.dto.user.UserRegisterRequestDto;
import task.system.dto.user.UserResponseDto;
import task.system.security.AuthenticationService;
import task.system.service.user.UserService;

@Tag(name = "Authentication management",
        description = "Endpoints for authorization and authentication users"
)
@Transactional
@RestController
@RequestMapping(value = "/auth")
public class AuthenticationController {
    private static final Logger LOGGER = LogManager.getLogger(AuthenticationController.class);

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Autowired
    public AuthenticationController(UserService userService,
                                    AuthenticationService authenticationService
    ) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @PostMapping(value = "/register")
    @Operation(summary = "Register", description = "Register user")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@RequestBody @Valid UserRegisterRequestDto request) {
        LOGGER.info("User with email: {} tried to register.", request.getEmail());
        return userService.register(request);
    }

    @PostMapping(value = "/login")
    @Operation(summary = "Login", description = "Login user")
    @ResponseStatus(HttpStatus.OK)
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto requestDto) {
        if (requestDto.getUsernameOrEmail().contains("@")) {
            LOGGER.info("User with email: {} tried to sign in system.",
                    requestDto.getUsernameOrEmail());
        } else {
            LOGGER.info("User with username: {} tried to sign in system.",
                    requestDto.getUsernameOrEmail());
        }

        return authenticationService.authenticate(requestDto);
    }
}
