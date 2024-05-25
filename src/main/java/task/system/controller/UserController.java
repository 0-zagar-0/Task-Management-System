package task.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import task.system.dto.user.UserResponseDto;
import task.system.dto.user.UserUpdateProfileRequest;
import task.system.service.user.UserService;

@Tag(name = "User management", description = "Endpoints for users action")
@RestController
@RequestMapping(value = "/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/me")
    @Operation(summary = "Get profile", description = "Get user profile")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDto getProfile() {
        return userService.getProfile();
    }

    @PutMapping(value = "/{id}/role")
    @Operation(summary = "Update role", description = "Update user role, admins access only")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN')")
    public UserResponseDto updateRole(@PathVariable Long id, @RequestParam String role) {
        return userService.updateRole(id, role);
    }

    @PutMapping(value = "/me")
    @Operation(summary = "Update profile", description = "Update user profile")
    @ResponseStatus(HttpStatus.OK)
    public UserResponseDto updateProfile(
            @RequestBody @Valid UserUpdateProfileRequest updateRequest
    ) {
        return userService.updateProfile(updateRequest);
    }
}
