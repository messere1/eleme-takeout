package cn.edu.tju.takeout.product;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class ProductBoundaryServiceTest {
    @Mock private ProductMapper productMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ShopMapper shopMapper;
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productMapper, categoryMapper, shopMapper);
    }

    @Test
    void missingCategoryRejectsProductCreation() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));
        when(categoryMapper.findById(30L)).thenReturn(Optional.empty());

        assertCode(() -> service.create(12L, 20L, request()), "RESOURCE_NOT_FOUND");
    }

    @Test
    void anotherMerchantCannotChangeProductStatus() {
        when(productMapper.findById(40L)).thenReturn(Optional.of(product()));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));

        assertCode(
                () -> service.changeStatus(99L, 40L, new ProductStatusRequest("ON_SALE")),
                "FORBIDDEN");
    }

    @Test
    void missingProductCannotBeDeleted() {
        when(productMapper.findById(40L)).thenReturn(Optional.empty());

        assertCode(() -> service.delete(12L, 40L), "RESOURCE_NOT_FOUND");
    }

    private static ProductRequest request() {
        return new ProductRequest(
                "煎饼果子", 30L, "现做现卖", new BigDecimal("8.50"), 20);
    }

    private static Product product() {
        return Product.of(
                40L, 20L, 30L, "煎饼果子", "现做现卖",
                new BigDecimal("8.50"), 20, "OFF_SALE");
    }

    private static Shop shop(Long merchantId) {
        Shop shop = Shop.initiallyClosed(merchantId, "北洋餐厅");
        shop.setId(20L);
        return shop;
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo(code);
    }
}
