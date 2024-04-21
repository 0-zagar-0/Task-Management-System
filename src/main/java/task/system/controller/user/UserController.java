package task.system.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import task.system.service.user.UserService;

@RestController
@RequestMapping(value = "/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/me")
    @ResponseStatus(HttpStatus.OK)
    public Object getProfile() {
        return userService.getProfile();
    }

    @PutMapping(value = "/{id}/role")
    @ResponseStatus(HttpStatus.OK)
    public Object updateRole(@PathVariable Long id, @RequestBody Object roleRequest) {
        return userService.updateRole(id, roleRequest);
    }

    @PutMapping(value = "/me")
    @ResponseStatus(HttpStatus.OK)
    public Object updateProfile(@RequestBody Object updateRequest) {
        return userService.updateProfile(updateRequest);
    }
}
