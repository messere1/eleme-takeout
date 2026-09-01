package cn.edu.tju.takeout.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.user.User;
import cn.edu.tju.takeout.user.UserMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserLoginServiceTest {
    @Mock
    private UserMapper userMapper;

    private BCryptPasswordEncoder encoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        authService = new AuthService(userMapper, encoder, new JwtService(
                "phase-one-test-secret-key-must-be-at-least-32-bytes", Duration.ofHours(2)));
    }

    @Test
    void logsInByUsernameAndIssuesTwoHourCustomerToken() {
        User user = User.registered("alice", "13800138000", encoder.encode("abc12345"));
        user.setId(7L);
        when(userMapper.findByAccount("alice")).thenReturn(Optional.of(user));

        LoginView result = authService.login(new LoginRequest("alice", "abc12345", "CUSTOMER"));
        JwtClaims claims = authService.parse(result.token());

        assertThat(result.role()).isEqualTo("CUSTOMER");
        assertThat(claims.subject()).isEqualTo("7");
        assertThat(claims.role()).isEqualTo("CUSTOMER");
        assertThat(Duration.between(Instant.now(), claims.expiresAt()))
                .isBetween(Duration.ofMinutes(119), Duration.ofHours(2));
    }

    @Test
    void rejectsWrongPasswordWithoutRevealingWhichCredentialFailed() {
        User user = User.registered("alice", "13800138000", encoder.encode("abc12345"));
        when(userMapper.findByAccount("13800138000")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("13800138000", "wrong123", "CUSTOMER")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误")
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("AUTH_INVALID");
    }
}

