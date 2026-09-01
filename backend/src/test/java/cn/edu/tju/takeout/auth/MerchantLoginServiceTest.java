package cn.edu.tju.takeout.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.merchant.Merchant;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.user.UserMapper;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MerchantLoginServiceTest {
    @Mock private UserMapper userMapper;
    @Mock private MerchantMapper merchantMapper;
    private BCryptPasswordEncoder encoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        authService = new AuthService(userMapper, merchantMapper, encoder,
                new JwtService("phase-one-test-secret-key-must-be-at-least-32-bytes", Duration.ofHours(2)));
    }

    @Test
    void merchantPhoneAndPasswordIssueMerchantToken() {
        Merchant merchant = Merchant.registered("北洋餐厅", "13800138000", encoder.encode("abc12345"), "中式快餐");
        merchant.setId(12L);
        when(merchantMapper.findByPhone("13800138000")).thenReturn(Optional.of(merchant));

        LoginView result = authService.login(new LoginRequest("13800138000", "abc12345", "MERCHANT"));
        JwtClaims claims = authService.parse(result.token());

        assertThat(result.role()).isEqualTo("MERCHANT");
        assertThat(claims.subject()).isEqualTo("12");
        assertThat(claims.role()).isEqualTo("MERCHANT");
    }

    @Test
    void unknownMerchantUsesSameGenericCredentialError() {
        when(merchantMapper.findByPhone("13900139000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("13900139000", "abc12345", "MERCHANT")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误")
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("AUTH_INVALID");
    }
}

