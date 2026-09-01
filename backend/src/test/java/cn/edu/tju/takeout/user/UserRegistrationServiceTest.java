package cn.edu.tju.takeout.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, new BCryptPasswordEncoder());
    }

    @Test
    void registersUserWithTrimmedNameAndBcryptPassword() {
        when(userMapper.findByUsername("alice")).thenReturn(Optional.empty());
        when(userMapper.findByPhone("13800138000")).thenReturn(Optional.empty());

        UserView result = userService.register(
                new UserRegistrationRequest("  alice  ", "13800138000", "abc12345"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(saved.getPasswordHash()).doesNotContain("abc12345");
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.phone()).isEqualTo("13800138000");
    }

    @Test
    void rejectsDuplicateUsernameBeforeSaving() {
        when(userMapper.findByUsername("alice"))
                .thenReturn(Optional.of(User.registered("alice", "13800138000", "hash")));

        assertThatThrownBy(() -> userService.register(
                new UserRegistrationRequest("alice", "13900139000", "abc12345")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("USER_ALREADY_EXISTS");
        verify(userMapper, never()).insert(any());
    }

    @Test
    void rejectsDuplicatePhoneBeforeSaving() {
        when(userMapper.findByUsername("alice")).thenReturn(Optional.empty());
        when(userMapper.findByPhone("13800138000"))
                .thenReturn(Optional.of(User.registered("bob", "13800138000", "hash")));

        assertThatThrownBy(() -> userService.register(
                new UserRegistrationRequest("alice", "13800138000", "abc12345")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("USER_ALREADY_EXISTS");
        verify(userMapper, never()).insert(any());
    }
}

