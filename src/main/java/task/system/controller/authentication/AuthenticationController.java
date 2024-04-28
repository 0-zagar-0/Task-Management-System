package task.system.controller.authentication;

import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.user.UserLoginRequestDto;
import task.system.dto.user.UserLoginResponseDto;
import task.system.dto.user.UserRegisterRequestDto;
import task.system.dto.user.UserResponseDto;
import task.system.security.AuthenticationService;
import task.system.service.user.UserService;

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
    public UserResponseDto register(@RequestBody @Valid UserRegisterRequestDto request) {
        LOGGER.info("User with email: {} tried to register.", request.getEmail());
        return userService.register(request);
    }

    @PostMapping(value = "/login")
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto requestDto) {
        LOGGER.info("User with email: {} tried to sign in system.", requestDto.getEmail());
        return authenticationService.authenticate(requestDto);
    }
}
