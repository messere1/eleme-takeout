package cn.edu.tju.takeout.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.auth.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderControllerContractTest {
    private final OrderService service = mock(OrderService.class);
    private final OrderController controller = new OrderController(service);
    private final UserPrincipal customer = new UserPrincipal(7L, "CUSTOMER");

    @Test
    void delegatesEveryOrderEndpointAndPreservesQueryParameters() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 8, 0);
        LocalDateTime end = start.plusDays(1);
        OrderView order = new OrderView(
                30L, "T20260901001", 20L, new BigDecimal("19.80"),
                "PENDING", start, List.of());
        OrderPage page = new OrderPage(List.of(), 2, 5, 0, 0);
        when(service.create(7L, "天津大学")).thenReturn(order);
        when(service.list(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(page);
        when(service.getDetail(7L, "CUSTOMER", 30L)).thenReturn(order);

        assertThat(controller.create(customer, new CreateOrderRequest("天津大学")).data())
                .isEqualTo(order);
        assertThat(controller.list(customer, "PENDING", start, end, 2, 5).data()).isEqualTo(page);
        assertThat(controller.getDetail(customer, 30L).data()).isEqualTo(order);

        ArgumentCaptor<OrderQuery> query = ArgumentCaptor.forClass(OrderQuery.class);
        verify(service).list(org.mockito.ArgumentMatchers.eq(7L), query.capture());
        assertThat(query.getValue()).isEqualTo(new OrderQuery("PENDING", start, end, 2, 5));
    }
}
