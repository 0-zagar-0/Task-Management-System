package task.system.service.user;

import task.system.dto.user.UserRegisterRequestDto;
import task.system.dto.user.UserResponseDto;
import task.system.dto.user.UserUpdateProfileRequest;
import task.system.model.User;

public interface UserService {
    UserResponseDto register(UserRegisterRequestDto requestDto);

    UserResponseDto getProfile();

    UserResponseDto updateRole(Long id, String role);

    UserResponseDto updateProfile(UserUpdateProfileRequest updateRequest);

    User getAuthenticatedUser();
}
