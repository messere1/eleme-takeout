package cn.edu.tju.takeout.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.product.ProductMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartQueryServiceTest {
    @Mock private CartMapper cartMapper;
    @Mock private ProductMapper productMapper;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartMapper, productMapper);
    }

    @Test
    void cartReturnsExactLineSubtotalsAndTotalAmount() {
        when(cartMapper.findDetailsByUserId(7L)).thenReturn(List.of(
                CartLine.of(1L, 40L, "煎饼果子", new BigDecimal("8.50"), 2, 20, "ON_SALE"),
                CartLine.of(2L, 41L, "豆浆", new BigDecimal("3.20"), 3, 10, "ON_SALE")));

        CartView result = cartService.get(7L);

        assertThat(result.items()).extracting(CartLineView::subtotal)
                .containsExactly(new BigDecimal("17.00"), new BigDecimal("9.60"));
        assertThat(result.totalAmount()).isEqualByComparingTo("26.60");
    }

    @Test
    void offSaleProductIsMarkedInCart() {
        when(cartMapper.findDetailsByUserId(7L)).thenReturn(List.of(
                CartLine.of(1L, 40L, "煎饼果子", new BigDecimal("8.50"), 2, 20, "OFF_SALE")));

        CartLineView item = cartService.get(7L).items().get(0);

        assertThat(item.available()).isFalse();
        assertThat(item.unavailableReason()).isEqualTo("OFF_SALE");
    }

    @Test
    void insufficientStockIsMarkedInCart() {
        when(cartMapper.findDetailsByUserId(7L)).thenReturn(List.of(
                CartLine.of(1L, 40L, "煎饼果子", new BigDecimal("8.50"), 5, 3, "ON_SALE")));

        CartLineView item = cartService.get(7L).items().get(0);

        assertThat(item.available()).isFalse();
        assertThat(item.unavailableReason()).isEqualTo("INSUFFICIENT_STOCK");
    }
}
