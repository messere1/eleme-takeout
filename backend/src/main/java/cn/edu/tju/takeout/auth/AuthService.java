package cn.edu.tju.takeout.auth;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.merchant.Merchant;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.user.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import cn.edu.tju.takeout.user.User;
import java.util.Optional;
import org.springframework.http.HttpStatus;
@Service
public class AuthService {
    private static final long EXPIRES_IN_SECONDS = 2 * 60 * 60;

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this(userMapper, null, passwordEncoder, jwtService);
    }

    @Autowired
    public AuthService(
            UserMapper userMapper, MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginView login(LoginRequest request) {
        String account = request.account().trim();
        String password = request.password();
        String role = request.role();

        if ("CUSTOMER".equals(role)) {

            User user = userMapper
                    .findByAccount(account)
                    .orElseThrow(this::invalidCredentials);

            if (!passwordEncoder.matches(
                    password,
                    user.getPasswordHash())) {

                throw invalidCredentials();
            }

            String token = jwtService.issue(
                    String.valueOf(user.getId()),
                    "CUSTOMER"
            );

            return new LoginView(
                    token,
                    "CUSTOMER",
                    EXPIRES_IN_SECONDS
            );
        }

        if ("MERCHANT".equals(role)) {

            if (merchantMapper == null) {
                throw invalidCredentials();
            }

            Optional<Merchant> merchant =
                    merchantMapper.findByName(account);

            if (merchant.isEmpty()) {
                merchant = merchantMapper.findByPhone(account);
            }

            Merchant currentMerchant =
                    merchant.orElseThrow(
                            this::invalidCredentials
                    );

            if (!passwordEncoder.matches(
                    password,
                    currentMerchant.getPasswordHash())) {

                throw invalidCredentials();
            }

            String token = jwtService.issue(
                    String.valueOf(
                            currentMerchant.getId()
                    ),
                    "MERCHANT"
            );

            return new LoginView(
                    token,
                    "MERCHANT",
                    EXPIRES_IN_SECONDS
            );
        }

        throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "角色不合法"
        );
    }

    public JwtClaims parse(String token) {
        return jwtService.parse(token);
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID",
                "账号或密码错误"
        );
    }
}
