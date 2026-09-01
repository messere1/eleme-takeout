package cn.edu.tju.takeout.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        // 仅保留依赖签名，等待功能开发人员实现。
    }

    public UserView register(UserRegistrationRequest request) {
        throw pending();
    }

    public UserView getProfile(Long userId) {
        throw pending();
    }

    public UserView updateProfile(Long userId, UserProfileUpdateRequest request) {
        throw pending();
    }

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：用户业务尚未实现");
    }
}
