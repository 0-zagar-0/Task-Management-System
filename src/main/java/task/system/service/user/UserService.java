package task.system.service.user;

import task.system.model.User;

public interface UserService {

    Object getProfile();

    Object updateRole(Long id, Object role);

    Object updateProfile(Object updateRequest);

    User getAuthenticatedUser();
}
