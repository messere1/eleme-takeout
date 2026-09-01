package cn.edu.tju.takeout.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.cart.CartMapper;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
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
class OrderDetailAuthorizationTest {
    @Mock private OrderMapper orderMapper;
    @Mock private CartMapper cartMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ShopMapper shopMapper;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderMapper, cartMapper, productMapper, shopMapper);
    }

    @Test
    void customerReadsOwnOrderWithCurrentStatusAndItems() {
        when(orderMapper.findById(60L)).thenReturn(Optional.of(order(7L)));
        when(orderMapper.findItemsByOrderId(60L)).thenReturn(List.of(item()));

        OrderView result = orderService.getDetail(7L, "CUSTOMER", 60L);

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.items()).singleElement()
                .extracting(OrderItemView::productName)
                .isEqualTo("煎饼果子");
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        when(orderMapper.findById(60L)).thenReturn(Optional.of(order(8L)));

        assertForbidden(() -> orderService.getDetail(7L, "CUSTOMER", 60L));
        verify(orderMapper, never()).findItemsByOrderId(60L);
    }

    @Test
    void merchantReadsOrderFromOwnShop() {
        when(orderMapper.findById(60L)).thenReturn(Optional.of(order(7L)));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));
        when(orderMapper.findItemsByOrderId(60L)).thenReturn(List.of(item()));

        OrderView result = orderService.getDetail(12L, "MERCHANT", 60L);

        assertThat(result.orderNo()).isEqualTo("NO-60");
    }

    @Test
    void merchantCannotReadOrderFromAnotherShop() {
        when(orderMapper.findById(60L)).thenReturn(Optional.of(order(7L)));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(13L)));

        assertForbidden(() -> orderService.getDetail(12L, "MERCHANT", 60L));
        verify(orderMapper, never()).findItemsByOrderId(60L);
    }

    private static void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("FORBIDDEN");
    }

    private static Order order(Long userId) {
        return Order.restore(
                60L, "NO-60", userId, 20L, new BigDecimal("17.00"),
                "PENDING", LocalDateTime.of(2026, 9, 1, 12, 0));
    }

    private static OrderItem item() {
        return OrderItem.restore(
                70L, 60L, 40L, "煎饼果子", new BigDecimal("8.50"),
                2, new BigDecimal("17.00"));
    }

    private static Shop shop(Long merchantId) {
        Shop shop = Shop.initiallyClosed(merchantId, "北洋餐厅");
        shop.setId(20L);
        return shop;
    }
}
