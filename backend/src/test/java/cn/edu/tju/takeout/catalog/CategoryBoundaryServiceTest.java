package cn.edu.tju.takeout.catalog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryBoundaryServiceTest {
    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ShopMapper shopMapper;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryMapper, productMapper, shopMapper);
    }

    @Test
    void missingShopRejectsCategoryCreation() {
        when(shopMapper.findById(20L)).thenReturn(Optional.empty());

        assertCode(() -> service.create(12L, 20L, request()), "RESOURCE_NOT_FOUND");
    }

    @Test
    void anotherMerchantCannotCreateCategoryForShop() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));

        assertCode(() -> service.create(99L, 20L, request()), "FORBIDDEN");
    }

    @Test
    void duplicateCategoryNameReturnsConflict() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));
        when(categoryMapper.findByName(20L, "主食"))
                .thenReturn(Optional.of(Category.of(30L, 20L, "主食", 2)));

        assertCode(() -> service.create(12L, 20L, request()), "BUSINESS_CONFLICT");
    }

    @Test
    void missingCategoryCannotBeUpdated() {
        when(categoryMapper.findById(30L)).thenReturn(Optional.empty());

        assertCode(() -> service.update(12L, 30L, request()), "RESOURCE_NOT_FOUND");
    }

    private static CategoryRequest request() {
        return new CategoryRequest("主食", 1);
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
