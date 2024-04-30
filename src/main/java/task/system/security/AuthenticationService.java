package task.system.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import task.system.dto.user.UserLoginRequestDto;
import task.system.dto.user.UserLoginResponseDto;

@Service
public class AuthenticationService {
    private static final Logger LOGGER = LogManager.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthenticationService(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public UserLoginResponseDto authenticate(UserLoginRequestDto requestDto) {
        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        requestDto.getUsernameOrEmail(),
                        requestDto.getPassword())
        );
        String token = jwtUtil.generateToken(authenticate.getName());

        if (requestDto.getUsernameOrEmail().contains("@")) {
            LOGGER.info("User with email: {} sign in completed",
                    requestDto.getUsernameOrEmail()
            );
        } else {
            LOGGER.info("User with username: {} sign in completed",
                    requestDto.getUsernameOrEmail());
        }

        return new UserLoginResponseDto(token);
    }
}
