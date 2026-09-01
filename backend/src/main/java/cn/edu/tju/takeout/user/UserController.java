package cn.edu.tju.takeout.user;

import cn.edu.tju.takeout.common.ApiResponse;
import cn.edu.tju.takeout.auth.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserView> register(@Valid @RequestBody UserRegistrationRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> profile(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.getProfile(principal.userId()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserView> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateProfile(principal.userId(), request));
    }
}
