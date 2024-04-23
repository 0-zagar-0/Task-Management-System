package task.system.service.user;

import task.system.dto.user.UserRegisterRequestDto;
import task.system.dto.user.UserResponseDto;
import task.system.model.User;

public interface UserService {
    UserResponseDto register(UserRegisterRequestDto requestDto);

    Object getProfile();

    Object updateRole(Long id, Object role);

    Object updateProfile(Object updateRequest);

    User getAuthenticatedUser();
}
