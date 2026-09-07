package cn.edu.tju.takeout.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.Product;
import cn.edu.tju.takeout.product.ProductMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartUpdateServiceTest {
    @Mock private CartMapper cartMapper;
    @Mock private ProductMapper productMapper;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartMapper, productMapper);
    }

    @Test
    void customerUpdatesItemQuantityWithinStock() {
        CartItem item = CartItem.of(50L, 7L, 40L, 2);
        when(cartMapper.findByIdAndUserId(50L, 7L)).thenReturn(Optional.of(item));
        when(productMapper.findById(40L)).thenReturn(Optional.of(product(5)));

        CartItemView result = cartService.update(7L, 50L, new UpdateCartRequest(4));

        verify(cartMapper).updateQuantity(item);
        assertThat(result.quantity()).isEqualTo(4);
    }

    @Test
    void zeroQuantityDeletesItem() {
        when(cartMapper.findByIdAndUserId(50L, 7L))
                .thenReturn(Optional.of(CartItem.of(50L, 7L, 40L, 2)));

        cartService.update(7L, 50L, new UpdateCartRequest(0));

        verify(cartMapper).deleteByIdAndUserId(50L, 7L);
    }

    @Test
    void quantityOverStockIsRejected() {
        when(cartMapper.findByIdAndUserId(50L, 7L))
                .thenReturn(Optional.of(CartItem.of(50L, 7L, 40L, 2)));
        when(productMapper.findById(40L)).thenReturn(Optional.of(product(3)));

        assertThatThrownBy(() -> cartService.update(7L, 50L, new UpdateCartRequest(4)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
    }

    @Test
    void customerCannotUpdateAnotherCustomersCartItem() {
        when(cartMapper.findByIdAndUserId(50L, 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.update(8L, 50L, new UpdateCartRequest(2)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    private static Product product(int stock) {
        return Product.of(
                40L, 20L, 30L, "煎饼果子", "现做现卖",
                new BigDecimal("8.50"), stock, "ON_SALE");
    }
}
