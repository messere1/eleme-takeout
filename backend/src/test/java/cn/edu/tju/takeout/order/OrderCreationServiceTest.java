package cn.edu.tju.takeout.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.cart.CartCheckoutLine;
import cn.edu.tju.takeout.cart.CartMapper;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTest {
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
    void closedShopRejectsOrderBeforePersistence() {
        when(cartMapper.findCheckoutLinesByUserId(7L)).thenReturn(List.of(line(2, 20)));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop("CLOSED")));

        assertThatThrownBy(() -> orderService.create(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void insufficientStockRejectsOrderBeforePersistence() {
        when(cartMapper.findCheckoutLinesByUserId(7L)).thenReturn(List.of(line(3, 2)));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop("OPEN")));

        assertThatThrownBy(() -> orderService.create(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void successfulOrderHasConsistentAmountAndClearsCart() {
        when(cartMapper.findCheckoutLinesByUserId(7L)).thenReturn(List.of(line(2, 20)));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop("OPEN")));
        when(productMapper.decreaseStockIfAvailable(40L, 2)).thenReturn(1);

        OrderView result = orderService.create(7L);

        verify(orderMapper).insert(any(Order.class));
        verify(orderMapper).insertItem(any(OrderItem.class));
        verify(productMapper).decreaseStockIfAvailable(40L, 2);
        verify(cartMapper).deleteByUserId(7L);
        assertThat(result.orderNo()).isNotBlank();
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.totalAmount()).isEqualByComparingTo("17.00");
        assertThat(result.items()).singleElement()
                .extracting(OrderItemView::subtotal)
                .isEqualTo(new BigDecimal("17.00"));
    }

    @Test
    void concurrentStockLossAbortsBeforeCartIsCleared() {
        when(cartMapper.findCheckoutLinesByUserId(7L)).thenReturn(List.of(line(2, 20)));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop("OPEN")));
        when(productMapper.decreaseStockIfAvailable(40L, 2)).thenReturn(0);

        assertThatThrownBy(() -> orderService.create(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
        verify(cartMapper, never()).deleteByUserId(7L);
    }

    private static CartCheckoutLine line(int quantity, int stock) {
        return CartCheckoutLine.of(
                50L, 20L, 40L, "煎饼果子", new BigDecimal("8.50"), quantity, stock, "ON_SALE");
    }

    private static Shop shop(String status) {
        Shop shop = Shop.initiallyClosed(12L, "北洋餐厅");
        shop.setId(20L);
        shop.changeStatus(status);
        return shop;
    }
}
