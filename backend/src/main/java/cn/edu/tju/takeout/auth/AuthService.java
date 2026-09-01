package cn.edu.tju.takeout.auth;

import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.user.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this(userMapper, null, passwordEncoder, jwtService);
    }

    @Autowired
    public AuthService(
            UserMapper userMapper, MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        // 仅保留依赖签名，等待功能开发人员实现。
    }

    public LoginView login(LoginRequest request) {
        throw pending();
    }

    public JwtClaims parse(String token) {
        throw pending();
    }

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：身份认证业务尚未实现");
    }
}
