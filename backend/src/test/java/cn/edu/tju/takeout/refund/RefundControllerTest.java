package cn.edu.tju.takeout.refund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import cn.edu.tju.takeout.admin.AdminMapper;
import cn.edu.tju.takeout.auth.UserPrincipal;
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

@ExtendWith(MockitoExtension.class)
class RefundControllerTest {
    @Mock private RefundMapper refunds;
    @Mock private OrderMapper orders;
    @Mock private AdminMapper admins;
    private RefundController controller;

    @BeforeEach void setUp() { controller = new RefundController(refunds, orders, admins); }

    @Test void refundRejectsForeignUnpaidAndExcessRequests() {
        when(orders.findByIdForUpdate(1L)).thenReturn(Optional.of(order(8L, false)));
        assertCode(() -> controller.create(new UserPrincipal(7L, "CUSTOMER"), 1L, request("1.00")), "FORBIDDEN");
        when(orders.findByIdForUpdate(2L)).thenReturn(Optional.of(order(7L, false)));
        assertCode(() -> controller.create(new UserPrincipal(7L, "CUSTOMER"), 2L, request("1.00")), "BUSINESS_CONFLICT");
        when(orders.findByIdForUpdate(3L)).thenReturn(Optional.of(order(7L, true)));
        when(refunds.reserved(3L)).thenReturn(new BigDecimal("9.50"));
        assertCode(() -> controller.create(new UserPrincipal(7L, "CUSTOMER"), 3L, request("1.00")), "BUSINESS_CONFLICT");
    }

    @Test void validPartialRefundUsesRemainingAmountAndPersistsEvidence() {
        when(orders.findByIdForUpdate(3L)).thenReturn(Optional.of(order(7L, true)));
        when(refunds.reserved(3L)).thenReturn(new BigDecimal("2.00"));
        RefundRequest result = controller.create(new UserPrincipal(7L, "CUSTOMER"), 3L,
                new CreateRefundRequest(new BigDecimal("8.00"), " 少放商品 ", List.of("/uploads/a.png"))).data();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getAmount()).isEqualByComparingTo("8.00");
        verify(refunds).insert(result);
    }

    @Test void concurrentOrRepeatedDecisionReturnsConflictAndSuccessfulAdminDecisionIsAudited() {
        RefundRequest pending = RefundRequest.pending(3L, 7L, BigDecimal.ONE, "原因", null);
        pending.setId(4L);
        when(refunds.findById(4L)).thenReturn(Optional.of(pending));
        when(refunds.decide(4L, "APPROVED")).thenReturn(0);
        assertCode(() -> controller.decide(new UserPrincipal(9L, "ADMIN"), 4L,
                new RefundDecisionRequest("APPROVED")), "BUSINESS_CONFLICT");

        RefundRequest approved = mock(RefundRequest.class);
        when(approved.getStatus()).thenReturn("APPROVED");
        when(refunds.findById(5L)).thenReturn(Optional.of(pending), Optional.of(approved));
        when(refunds.decide(5L, "APPROVED")).thenReturn(1);
        assertThat(controller.decide(new UserPrincipal(9L, "ADMIN"), 5L,
                new RefundDecisionRequest("APPROVED")).data().getStatus()).isEqualTo("APPROVED");
        verify(admins).audit(9L, "SET_STATUS", "REFUND", 5L);
    }

    @Test void merchantCannotDecideAnotherShopsRefund() {
        RefundRequest pending = RefundRequest.pending(3L, 7L, BigDecimal.ONE, "原因", null);
        pending.setId(4L);
        when(refunds.findById(4L)).thenReturn(Optional.of(pending));
        when(refunds.decideForMerchant(4L, 12L, "REJECTED")).thenReturn(0);
        assertCode(() -> controller.merchantDecide(new UserPrincipal(12L, "MERCHANT"), 4L,
                new RefundDecisionRequest("REJECTED")), "FORBIDDEN");
    }

    private CreateRefundRequest request(String amount) {
        return new CreateRefundRequest(new BigDecimal(amount), "退款原因", List.of());
    }
    private Order order(long userId, boolean paid) {
        Order order = Order.restore(3L, "O3", userId, 20L, BigDecimal.TEN, "COMPLETED", LocalDateTime.now());
        if (paid) order.markPaid(LocalDateTime.now());
        return order;
    }
    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code()).isEqualTo(code);
    }
}
