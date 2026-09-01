package cn.edu.tju.takeout.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {
    @Mock
    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, new BCryptPasswordEncoder());
    }

    @Test
    void readsCurrentUsersProfile() {
        User user = user(7L, "alice", "13800138000");
        when(userMapper.findById(7L)).thenReturn(Optional.of(user));

        UserView result = userService.getProfile(7L);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.phone()).isEqualTo("13800138000");
    }

    @Test
    void updatesNicknamePhoneAndAddressForCurrentUser() {
        User user = user(7L, "alice", "13800138000");
        when(userMapper.findById(7L)).thenReturn(Optional.of(user));
        when(userMapper.findByPhone("13900139000")).thenReturn(Optional.empty());

        UserView result = userService.updateProfile(7L,
                new UserProfileUpdateRequest("Alice同学", "13900139000", "天津大学北洋园校区"));

        verify(userMapper).updateProfile(user);
        assertThat(result.nickname()).isEqualTo("Alice同学");
        assertThat(result.phone()).isEqualTo("13900139000");
        assertThat(result.address()).isEqualTo("天津大学北洋园校区");
    }

    @Test
    void rejectsPhoneOwnedByAnotherUser() {
        User current = user(7L, "alice", "13800138000");
        User other = user(8L, "bob", "13900139000");
        when(userMapper.findById(7L)).thenReturn(Optional.of(current));
        when(userMapper.findByPhone("13900139000")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.updateProfile(7L,
                new UserProfileUpdateRequest("Alice", "13900139000", "天津")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("USER_ALREADY_EXISTS");
    }

    @Test
    void missingCurrentUserReturnsNotFound() {
        when(userMapper.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    private static User user(Long id, String username, String phone) {
        User user = User.registered(username, phone, "hash");
        user.setId(id);
        return user;
    }
}

