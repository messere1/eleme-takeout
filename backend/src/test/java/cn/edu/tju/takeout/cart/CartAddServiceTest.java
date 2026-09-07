package cn.edu.tju.takeout.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
class CartAddServiceTest {
    @Mock private CartMapper cartMapper;
    @Mock private ProductMapper productMapper;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartMapper, productMapper);
    }

    @Test
    void firstAddCreatesCartItem() {
        when(productMapper.findById(40L)).thenReturn(Optional.of(product("ON_SALE")));
        when(cartMapper.findByUserAndProduct(7L, 40L)).thenReturn(Optional.empty());

        CartItemView result = cartService.add(7L, new AddCartRequest(40L, 2));

        verify(cartMapper).insert(any(CartItem.class));
        assertThat(result.quantity()).isEqualTo(2);
    }

    @Test
    void repeatedAddAccumulatesQuantity() {
        CartItem existing = CartItem.of(50L, 7L, 40L, 2);
        when(productMapper.findById(40L)).thenReturn(Optional.of(product("ON_SALE")));
        when(cartMapper.findByUserAndProduct(7L, 40L)).thenReturn(Optional.of(existing));

        CartItemView result = cartService.add(7L, new AddCartRequest(40L, 3));

        verify(cartMapper).updateQuantity(existing);
        assertThat(result.quantity()).isEqualTo(5);
    }

    @Test
    void offSaleProductCannotBeAdded() {
        when(productMapper.findById(40L)).thenReturn(Optional.of(product("OFF_SALE")));

        assertThatThrownBy(() -> cartService.add(7L, new AddCartRequest(40L, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
    }

    @Test
    void repeatedAddCannotRaiseQuantityAboveCurrentStock() {
        CartItem existing = CartItem.of(50L, 7L, 40L, 4);
        when(productMapper.findById(40L)).thenReturn(Optional.of(product("ON_SALE")));
        when(cartMapper.findByUserAndProduct(7L, 40L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.add(7L, new AddCartRequest(40L, 17)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
        assertThat(existing.getQuantity()).isEqualTo(4);
    }

    @Test
    void sameProductInAnotherCustomersCartCreatesAnIndependentItem() {
        when(productMapper.findById(40L)).thenReturn(Optional.of(product("ON_SALE")));
        when(cartMapper.findByUserAndProduct(8L, 40L)).thenReturn(Optional.empty());

        cartService.add(8L, new AddCartRequest(40L, 1));

        org.mockito.ArgumentCaptor<CartItem> item =
                org.mockito.ArgumentCaptor.forClass(CartItem.class);
        verify(cartMapper).insert(item.capture());
        assertThat(item.getValue().getUserId()).isEqualTo(8L);
        assertThat(item.getValue().getProductId()).isEqualTo(40L);
    }

    private static Product product(String status) {
        return Product.of(
                40L, 20L, 30L, "煎饼果子", "现做现卖",
                new BigDecimal("8.50"), 20, status);
    }
}
