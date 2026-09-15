package cn.edu.tju.takeout.user;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.order.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAccountDeletionServiceTest {
    @Mock private UserMapper users;
    @Mock private PasswordEncoder passwords;
    @Mock private OrderMapper orders;
    private UserService service;

    @BeforeEach void setUp() { service = new UserService(users, passwords, orders); }

    @Test void activeOrderPreventsDeletion() {
        when(orders.countActiveByUser(7L)).thenReturn(1L);
        assertThatThrownBy(() -> service.deleteAccount(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
        verify(users, never()).softDelete(7L);
    }

    @Test void successfulDeletionAnonymizesThroughMapperAndInvalidatesActiveLookup() {
        when(orders.countActiveByUser(7L)).thenReturn(0L);
        when(users.softDelete(7L)).thenReturn(1);
        assertThatCode(() -> service.deleteAccount(7L)).doesNotThrowAnyException();
        verify(users).softDelete(7L);
    }

    @Test void repeatedDeletionReturnsNotFound() {
        when(orders.countActiveByUser(7L)).thenReturn(0L);
        when(users.softDelete(7L)).thenReturn(0);
        assertThatThrownBy(() -> service.deleteAccount(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }
}
