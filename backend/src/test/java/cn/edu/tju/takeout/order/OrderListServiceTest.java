package cn.edu.tju.takeout.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.cart.CartMapper;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.ShopMapper;
import cn.edu.tju.takeout.user.UserMapper;
import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderListServiceTest {
    @Mock private OrderMapper orderMapper;
    @Mock private CartMapper cartMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ShopMapper shopMapper;
    @Mock private UserMapper userMapper;
    @Mock private MerchantMapper merchantMapper;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderMapper, cartMapper, productMapper, shopMapper, userMapper, merchantMapper);
    }

    @Test
    void listIsScopedToCurrentCustomerAndKeepsDescendingDatabaseOrder() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 2, 0, 0);
        OrderQuery query = new OrderQuery("PENDING", start, end, 1, 2);
        when(orderMapper.findPageByUserId(7L, "PENDING", start, end, 2, 0))
                .thenReturn(List.of(order(2L, "NO-2", end.minusHours(1)),
                        order(1L, "NO-1", start.plusHours(1))));
        when(orderMapper.countByUserId(7L, "PENDING", start, end)).thenReturn(2L);

        OrderPage result = orderService.list(7L, query);

        assertThat(result.items()).extracting(OrderSummaryView::orderNo)
                .containsExactly("NO-2", "NO-1");
        verify(orderMapper).findPageByUserId(7L, "PENDING", start, end, 2, 0);
    }

    @Test
    void listReturnsCorrectPaginationMetadata() {
        OrderQuery query = new OrderQuery(null, null, null, 2, 10);
        when(orderMapper.findPageByUserId(7L, null, null, null, 10, 10))
                .thenReturn(List.of());
        when(orderMapper.countByUserId(7L, null, null, null)).thenReturn(21L);

        OrderPage result = orderService.list(7L, query);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.total()).isEqualTo(21L);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void omittedPaginationUsesSrsDefaults() {
        OrderQuery query = new OrderQuery(null, null, null, null, null);
        when(orderMapper.findPageByUserId(7L, null, null, null, 20, 0))
                .thenReturn(List.of());
        when(orderMapper.countByUserId(7L, null, null, null)).thenReturn(0L);

        OrderPage result = orderService.list(7L, query);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        verify(orderMapper).findPageByUserId(7L, null, null, null, 20, 0);
    }

    @Test
    void reversedTimeRangeIsRejectedBeforeQueryingDatabase() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 2, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 1, 0, 0);

        assertThatThrownBy(() -> orderService.list(
                7L, new OrderQuery("CREATED", start, end, 1, 20)))
                .isInstanceOf(cn.edu.tju.takeout.common.BusinessException.class)
                .extracting(error -> ((cn.edu.tju.takeout.common.BusinessException) error).code())
                .isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void mapperUsesDeterministicOrderWhenCreationTimesAreEqual() throws Exception {
        Method method = OrderMapper.class.getDeclaredMethod(
                "findPageByUserId", Long.class, String.class, LocalDateTime.class,
                LocalDateTime.class, Integer.class, Integer.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value())
                .replaceAll("\\s+", " ")
                .toUpperCase();

        assertThat(sql).contains("ORDER BY CREATED_AT DESC, ID DESC");
    }

    private static Order order(Long id, String orderNo, LocalDateTime createdAt) {
        return Order.restore(
                id, orderNo, 7L, 20L, new BigDecimal("17.00"), "PENDING", createdAt);
    }
}
