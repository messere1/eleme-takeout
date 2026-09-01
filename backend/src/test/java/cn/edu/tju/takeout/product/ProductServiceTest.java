package cn.edu.tju.takeout.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.catalog.Category;
import cn.edu.tju.takeout.catalog.CategoryMapper;
import cn.edu.tju.takeout.common.BusinessException;
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
class ProductServiceTest {
    @Mock private ProductMapper productMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ShopMapper shopMapper;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productMapper, categoryMapper, shopMapper);
    }

    @Test
    void ownerCreatesInitiallyOffSaleProductInOwnCategory() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));
        when(categoryMapper.findById(30L))
                .thenReturn(Optional.of(Category.of(30L, 20L, "主食", 1)));

        ProductView result = productService.create(12L, 20L, request("煎饼果子", 30L));

        verify(productMapper).insert(any(Product.class));
        assertThat(result.name()).isEqualTo("煎饼果子");
        assertThat(result.status()).isEqualTo("OFF_SALE");
    }

    @Test
    void categoryFromAnotherShopCannotBeUsed() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));
        when(categoryMapper.findById(30L))
                .thenReturn(Optional.of(Category.of(30L, 99L, "主食", 1)));

        assertThatThrownBy(() -> productService.create(12L, 20L, request("煎饼果子", 30L)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
    }

    @Test
    void ownerUpdatesProductDetails() {
        Product product = product(40L, 20L, 30L, "煎饼果子", "OFF_SALE");
        when(productMapper.findById(40L)).thenReturn(Optional.of(product));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));
        when(categoryMapper.findById(31L))
                .thenReturn(Optional.of(Category.of(31L, 20L, "套餐", 2)));

        ProductView result = productService.update(12L, 40L, request("双蛋煎饼", 31L));

        verify(productMapper).update(product);
        assertThat(result.name()).isEqualTo("双蛋煎饼");
        assertThat(result.categoryId()).isEqualTo(31L);
    }

    @Test
    void ownerChangesSaleStatus() {
        Product product = product(40L, 20L, 30L, "煎饼果子", "OFF_SALE");
        when(productMapper.findById(40L)).thenReturn(Optional.of(product));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));

        ProductView result = productService.changeStatus(12L, 40L, new ProductStatusRequest("ON_SALE"));

        verify(productMapper).updateStatus(product);
        assertThat(result.status()).isEqualTo("ON_SALE");
    }

    @Test
    void deletingProductUsesLogicalDeletion() {
        Product product = product(40L, 20L, 30L, "煎饼果子", "OFF_SALE");
        when(productMapper.findById(40L)).thenReturn(Optional.of(product));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L)));

        productService.delete(12L, 40L);

        verify(productMapper).logicalDelete(40L);
    }

    @Test
    void publicListOnlyUsesVisibleProductQuery() {
        when(productMapper.findVisibleByShopAndCategory(20L, 30L))
                .thenReturn(List.of(product(40L, 20L, 30L, "煎饼果子", "ON_SALE")));

        assertThat(productService.listVisible(20L, 30L))
                .extracting(ProductView::name)
                .containsExactly("煎饼果子");
    }

    private static ProductRequest request(String name, Long categoryId) {
        return new ProductRequest(name, categoryId, "现做现卖", new BigDecimal("8.50"), 20);
    }

    private static Product product(
            Long id, Long shopId, Long categoryId, String name, String status) {
        return Product.of(
                id, shopId, categoryId, name, "现做现卖", new BigDecimal("8.50"), 20, status);
    }

    private static Shop shop(Long merchantId) {
        Shop shop = Shop.initiallyClosed(merchantId, "北洋餐厅");
        shop.setId(20L);
        return shop;
    }
}
