package cn.edu.tju.takeout.rider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.order.Order;
import cn.edu.tju.takeout.order.OrderMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RiderServiceTest {
    @Mock private OrderMapper orders;
    @Mock private RiderMapper riders;
    @Mock private PasswordEncoder passwords;
    private RiderService service;

    @BeforeEach void setUp() { service = new RiderService(orders, riders, passwords); }

    @Test void registrationRejectsDuplicateNameAndPhone() {
        Rider existing = Rider.registered("骑手甲", "13700000001", "hash");
        when(riders.findByName("骑手甲")).thenReturn(Optional.of(existing));
        assertCode(() -> service.register(request("骑手甲", "13700000002")), "RIDER_ALREADY_EXISTS");

        when(riders.findByName("骑手乙")).thenReturn(Optional.empty());
        when(riders.findByPhone("13700000001")).thenReturn(Optional.of(existing));
        assertCode(() -> service.register(request("骑手乙", "13700000001")), "RIDER_ALREADY_EXISTS");
    }

    @Test void registrationHashesPasswordAndPersistsRider() {
        when(riders.findByName("骑手甲")).thenReturn(Optional.empty());
        when(riders.findByPhone("13700000001")).thenReturn(Optional.empty());
        when(passwords.encode("abc12345")).thenReturn("hash");
        RiderView view = service.register(request(" 骑手甲 ", "13700000001"));
        assertThat(view.riderName()).isEqualTo("骑手甲");
        verify(riders).insertRegistration(org.mockito.ArgumentMatchers.argThat(r -> "hash".equals(r.getPasswordHash())));
    }

    @Test void onlyOneRiderCanClaimAndOnlyOwnerCanDeliver() {
        Order claimed = order(9L, "DELIVERING");
        when(orders.claimForDelivery(9L, 2L)).thenReturn(1);
        when(orders.findById(9L)).thenReturn(Optional.of(claimed));
        assertThat(service.claim(2L, 9L).id()).isEqualTo(9L);
        when(orders.claimForDelivery(9L, 3L)).thenReturn(0);
        assertCode(() -> service.claim(3L, 9L), "BUSINESS_CONFLICT");

        when(orders.markDelivered(9L, 3L)).thenReturn(0);
        assertCode(() -> service.deliver(3L, 9L), "BUSINESS_CONFLICT");
    }

    @Test void successfulDeliveryReturnsUpdatedTaskAndMissingPostUpdateOrderIsNotFound() {
        when(orders.markDelivered(9L, 2L)).thenReturn(1);
        when(orders.findById(9L)).thenReturn(Optional.of(order(9L, "DELIVERED")));
        assertThat(service.deliver(2L, 9L).status()).isEqualTo("DELIVERED");

        when(orders.claimForDelivery(10L, 2L)).thenReturn(1);
        when(orders.findById(10L)).thenReturn(Optional.empty());
        assertCode(() -> service.claim(2L, 10L), "RESOURCE_NOT_FOUND");
    }

    private RiderRegistrationRequest request(String name, String phone) {
        return new RiderRegistrationRequest(name, phone, "abc12345");
    }
    private Order order(long id, String status) {
        Order order = Order.restore(id, "R" + id, 7L, 20L, BigDecimal.TEN, status, LocalDateTime.now());
        order.markPaid(LocalDateTime.now());
        order.setRiderId(2L);
        return order;
    }
    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code()).isEqualTo(code);
    }
}
