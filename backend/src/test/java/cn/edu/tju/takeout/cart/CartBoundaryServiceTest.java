package cn.edu.tju.takeout.cart;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartBoundaryServiceTest {
    @Mock private CartMapper cartMapper;
    @Mock private ProductMapper productMapper;
    @InjectMocks private CartService service;

    @Test
    void missingProductCannotBeAddedToCart() {
        when(productMapper.findById(40L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(7L, new AddCartRequest(40L, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("RESOURCE_NOT_FOUND");
    }
}
