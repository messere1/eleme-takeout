package cn.edu.tju.takeout.cart;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartDeleteServiceTest {
    @Mock private CartMapper cartMapper;
    @Mock private ProductMapper productMapper;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartMapper, productMapper);
    }

    @Test
    void customerDeletesOwnCartItem() {
        when(cartMapper.deleteByIdAndUserId(50L, 7L)).thenReturn(1);

        cartService.delete(7L, 50L);

        verify(cartMapper).deleteByIdAndUserId(50L, 7L);
    }

    @Test
    void deletingMissingOrForeignItemReturnsNotFound() {
        when(cartMapper.deleteByIdAndUserId(50L, 7L)).thenReturn(0);

        assertThatThrownBy(() -> cartService.delete(7L, 50L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void customerClearsOnlyOwnCart() {
        cartService.clear(7L);

        verify(cartMapper).deleteByUserId(7L);
    }
}
