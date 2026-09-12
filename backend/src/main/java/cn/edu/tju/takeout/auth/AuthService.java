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
import cn.edu.tju.takeout.admin.AdminMapper;
import cn.edu.tju.takeout.admin.Admin;
import cn.edu.tju.takeout.rider.RiderMapper;
import cn.edu.tju.takeout.rider.Rider;
import org.springframework.http.HttpStatus;
@Service
public class AuthService {
    private static final long EXPIRES_IN_SECONDS = 2 * 60 * 60;

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AdminMapper adminMapper;
    private final RiderMapper riderMapper;
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this(userMapper, null, passwordEncoder, jwtService, null, null);
    }

    public AuthService(
            UserMapper userMapper, MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder, JwtService jwtService) {
        this(userMapper, merchantMapper, passwordEncoder, jwtService, null, null);
    }

    @Autowired
    public AuthService(UserMapper userMapper, MerchantMapper merchantMapper,
            PasswordEncoder passwordEncoder, JwtService jwtService,
            AdminMapper adminMapper, RiderMapper riderMapper) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminMapper = adminMapper;
        this.riderMapper = riderMapper;
    }

    public LoginView login(LoginRequest request) {
        String account = request.account().trim();
        String password = request.password();
        String role = request.role();

        if ("CUSTOMER".equals(role)) {
            User user = userMapper
                    .findByAccount(account)
                    .orElseThrow(this::invalidCredentials);

            if (!user.isEnabled()
                    || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw invalidCredentials();
            }

            return loginView(user.getId(), "CUSTOMER");
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

            if (!currentMerchant.isEnabled()
                    || !passwordEncoder.matches(password, currentMerchant.getPasswordHash())) {
                throw invalidCredentials();
            }

            return loginView(currentMerchant.getId(), "MERCHANT");
        }

        if ("ADMIN".equals(role)) {
            if (adminMapper == null) {
                throw invalidCredentials();
            }
            Admin admin = adminMapper.findByAccount(account).orElseThrow(this::invalidCredentials);
            if (!admin.isEnabled()
                    || !passwordEncoder.matches(password, admin.getPasswordHash())) {
                throw invalidCredentials();
            }
            return loginView(admin.getId(), "ADMIN");
        }

        if ("RIDER".equals(role)) {
            if (riderMapper == null) {
                throw invalidCredentials();
            }
            Rider rider = riderMapper.findByAccount(account).orElseThrow(this::invalidCredentials);
            if (!rider.isEnabled()
                    || !passwordEncoder.matches(password, rider.getPasswordHash())) {
                throw invalidCredentials();
            }
            return loginView(rider.getId(), "RIDER");
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

    private LoginView loginView(Long id, String role) {
        return new LoginView(
                jwtService.issue(String.valueOf(id), role),
                role,
                EXPIRES_IN_SECONDS
        );
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID",
                "用户名或密码错误"
        );
    }
}
