package cn.edu.tju.takeout.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.catalog.CategoryMapper;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductStockServiceTest {
    @Mock private ProductMapper productMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ShopMapper shopMapper;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productMapper, categoryMapper, shopMapper);
    }

    @Test
    void ownerUpdatesAvailableStock() {
        Product product = product(20);
        when(productMapper.findById(40L)).thenReturn(Optional.of(product));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop()));

        ProductView result = productService.updateStock(12L, 40L, new StockRequest(15));

        verify(productMapper).updateStock(product);
        assertThat(result.stock()).isEqualTo(15);
    }

    @Test
    void negativeStockIsRejectedBeforeDatabaseWrite() {
        assertThatThrownBy(() -> productService.updateStock(12L, 40L, new StockRequest(-1)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("VALIDATION_ERROR");
        verify(productMapper, never()).updateStock(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void orderStockDeductionUsesAtomicConditionalUpdate() {
        when(productMapper.decreaseStockIfAvailable(40L, 3)).thenReturn(1);

        productService.decreaseStock(40L, 3);

        verify(productMapper).decreaseStockIfAvailable(40L, 3);
    }

    @Test
    void insufficientStockReturnsBusinessConflict() {
        when(productMapper.decreaseStockIfAvailable(40L, 21)).thenReturn(0);

        assertThatThrownBy(() -> productService.decreaseStock(40L, 21))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
    }

    private static Product product(int stock) {
        return Product.of(
                40L, 20L, 30L, "煎饼果子", "现做现卖",
                new BigDecimal("8.50"), stock, "ON_SALE");
    }

    private static Shop shop() {
        Shop shop = Shop.initiallyClosed(12L, "北洋餐厅");
        shop.setId(20L);
        return shop;
    }
}
