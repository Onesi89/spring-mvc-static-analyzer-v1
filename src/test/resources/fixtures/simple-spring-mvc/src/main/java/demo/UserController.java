package demo;

import org.springframework.stereotype.Controller;

@Controller
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void createUser() {
        userService.validate();
        userService.createUser();
    }

    private void helper() {
    }
}
