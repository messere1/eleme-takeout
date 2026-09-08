package cn.edu.tju.takeout.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductQueryAndPriceServiceTest {
    @Mock private ProductMapper productMapper;
    @Mock private CategoryMapper categoryMapper;
    @Mock private ShopMapper shopMapper;
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productMapper, categoryMapper, shopMapper);
    }

    @Test
    void publicCategoryPageUsesDefaultsAndReturnsMetadata() {
        when(categoryMapper.findById(30L)).thenReturn(Optional.of(category(30L, 20L)));
        when(productMapper.findVisiblePageByCategoryId(30L, 20, 0))
                .thenReturn(List.of(product(40L, 20L, 30L, "ON_SALE")));
        when(productMapper.countVisibleByCategoryId(30L)).thenReturn(21L);

        ProductPage result = service.listVisible(30L, null, null);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.items()).extracting(ProductView::id).containsExactly(40L);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void publicCategoryPageRejectsInvalidPage(int page) {
        assertCode(() -> service.listVisible(30L, page, 20), "VALIDATION_ERROR");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void publicCategoryPageRejectsInvalidSize(int size) {
        assertCode(() -> service.listVisible(30L, 1, size), "VALIDATION_ERROR");
    }

    @Test
    void publicCategoryPageRejectsMissingCategory() {
        when(categoryMapper.findById(30L)).thenReturn(Optional.empty());
        assertCode(() -> service.listVisible(30L, 1, 20), "RESOURCE_NOT_FOUND");
    }

    @Test
    void visibleDetailReturnsOnlyMapperVisibleProduct() {
        Product product = product(40L, 20L, 30L, "ON_SALE");
        when(productMapper.findVisibleById(40L)).thenReturn(Optional.of(product));

        assertThat(service.getVisibleProduct(40L).id()).isEqualTo(40L);
    }

    @Test
    void hiddenOrMissingProductDetailReturnsNotFound() {
        when(productMapper.findVisibleById(40L)).thenReturn(Optional.empty());
        assertCode(() -> service.getVisibleProduct(40L), "RESOURCE_NOT_FOUND");
    }

    @Test
    void ownerUpdatesPriceWithoutChangingOtherFields() {
        Product product = product(40L, 20L, 30L, "ON_SALE");
        when(productMapper.findById(40L)).thenReturn(Optional.of(product));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L, 20L)));

        ProductView result = service.updatePrice(
                12L, 40L, new ProductPriceRequest(new BigDecimal("9.90")));

        assertThat(result.price()).isEqualByComparingTo("9.90");
        verify(productMapper).updatePrice(product);
    }

    @Test
    void priceUpdateRejectsMissingProductShopAndForeignMerchant() {
        when(productMapper.findById(1L)).thenReturn(Optional.empty());
        assertCode(() -> service.updatePrice(
                12L, 1L, new ProductPriceRequest(new BigDecimal("9.90"))),
                "RESOURCE_NOT_FOUND");

        Product missingShopProduct = product(2L, 20L, 30L, "ON_SALE");
        when(productMapper.findById(2L)).thenReturn(Optional.of(missingShopProduct));
        when(shopMapper.findById(20L)).thenReturn(Optional.empty());
        assertCode(() -> service.updatePrice(
                12L, 2L, new ProductPriceRequest(new BigDecimal("9.90"))),
                "RESOURCE_NOT_FOUND");

        Product foreignProduct = product(3L, 21L, 30L, "ON_SALE");
        when(productMapper.findById(3L)).thenReturn(Optional.of(foreignProduct));
        when(shopMapper.findById(21L)).thenReturn(Optional.of(shop(99L, 21L)));
        assertCode(() -> service.updatePrice(
                12L, 3L, new ProductPriceRequest(new BigDecimal("9.90"))), "FORBIDDEN");
    }

    @Test
    void merchantListsAllProductsFromOwnShop() {
        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.of(shop(12L, 20L)));
        when(productMapper.findAllByShopId(20L)).thenReturn(List.of(
                product(40L, 20L, 30L, "ON_SALE"),
                product(41L, 20L, 30L, "OFF_SALE")));

        assertThat(service.listForMerchant(12L)).extracting(ProductView::id)
                .containsExactly(40L, 41L);
    }

    @Test
    void merchantWithoutShopCannotListProducts() {
        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.empty());
        assertCode(() -> service.listForMerchant(12L), "RESOURCE_NOT_FOUND");
    }

    @Test
    void createRejectsMissingShopAndForeignMerchant() {
        when(shopMapper.findById(1L)).thenReturn(Optional.empty());
        assertCode(() -> service.create(12L, 1L, request(30L)), "RESOURCE_NOT_FOUND");

        when(shopMapper.findById(2L)).thenReturn(Optional.of(shop(99L, 2L)));
        assertCode(() -> service.create(12L, 2L, request(30L)), "FORBIDDEN");
    }

    @Test
    void updateRejectsMissingResourcesOwnershipAndCrossShopCategory() {
        when(productMapper.findById(1L)).thenReturn(Optional.empty());
        assertCode(() -> service.update(12L, 1L, request(30L)), "RESOURCE_NOT_FOUND");

        Product missingShop = product(2L, 20L, 30L, "OFF_SALE");
        when(productMapper.findById(2L)).thenReturn(Optional.of(missingShop));
        when(shopMapper.findById(20L)).thenReturn(Optional.empty());
        assertCode(() -> service.update(12L, 2L, request(30L)), "RESOURCE_NOT_FOUND");

        Product foreign = product(3L, 21L, 30L, "OFF_SALE");
        when(productMapper.findById(3L)).thenReturn(Optional.of(foreign));
        when(shopMapper.findById(21L)).thenReturn(Optional.of(shop(99L, 21L)));
        assertCode(() -> service.update(12L, 3L, request(30L)), "FORBIDDEN");

        Product noCategory = product(4L, 22L, 30L, "OFF_SALE");
        when(productMapper.findById(4L)).thenReturn(Optional.of(noCategory));
        when(shopMapper.findById(22L)).thenReturn(Optional.of(shop(12L, 22L)));
        when(categoryMapper.findById(31L)).thenReturn(Optional.empty());
        assertCode(() -> service.update(12L, 4L, request(31L)), "RESOURCE_NOT_FOUND");

        Product crossShop = product(5L, 23L, 30L, "OFF_SALE");
        when(productMapper.findById(5L)).thenReturn(Optional.of(crossShop));
        when(shopMapper.findById(23L)).thenReturn(Optional.of(shop(12L, 23L)));
        when(categoryMapper.findById(32L)).thenReturn(Optional.of(category(32L, 99L)));
        assertCode(() -> service.update(12L, 5L, request(32L)), "BUSINESS_CONFLICT");
    }

    @Test
    void statusAndDeleteRejectMissingShopOrForeignMerchant() {
        Product statusProduct = product(1L, 20L, 30L, "OFF_SALE");
        when(productMapper.findById(1L)).thenReturn(Optional.of(statusProduct));
        when(shopMapper.findById(20L)).thenReturn(Optional.empty());
        assertCode(() -> service.changeStatus(
                12L, 1L, new ProductStatusRequest("ON_SALE")), "RESOURCE_NOT_FOUND");

        Product deleteProduct = product(2L, 21L, 30L, "OFF_SALE");
        when(productMapper.findById(2L)).thenReturn(Optional.of(deleteProduct));
        when(shopMapper.findById(21L)).thenReturn(Optional.of(shop(99L, 21L)));
        assertCode(() -> service.delete(12L, 2L), "FORBIDDEN");
    }

    @Test
    void statusDeleteAndStockReturnNotFoundForMissingResources() {
        when(productMapper.findById(10L)).thenReturn(Optional.empty());
        assertCode(() -> service.changeStatus(
                12L, 10L, new ProductStatusRequest("ON_SALE")), "RESOURCE_NOT_FOUND");

        Product deleteProduct = product(11L, 20L, 30L, "OFF_SALE");
        when(productMapper.findById(11L)).thenReturn(Optional.of(deleteProduct));
        when(shopMapper.findById(20L)).thenReturn(Optional.empty());
        assertCode(() -> service.delete(12L, 11L), "RESOURCE_NOT_FOUND");

        when(productMapper.findById(12L)).thenReturn(Optional.empty());
        assertCode(() -> service.updateStock(12L, 12L, new StockRequest(10)),
                "RESOURCE_NOT_FOUND");

        Product stockProduct = product(13L, 21L, 30L, "OFF_SALE");
        when(productMapper.findById(13L)).thenReturn(Optional.of(stockProduct));
        when(shopMapper.findById(21L)).thenReturn(Optional.empty());
        assertCode(() -> service.updateStock(12L, 13L, new StockRequest(10)),
                "RESOURCE_NOT_FOUND");
    }

    private static ProductRequest request(Long categoryId) {
        return new ProductRequest(
                "煎饼果子", categoryId, "现做现卖", new BigDecimal("8.50"), 20);
    }

    private static Product product(Long id, Long shopId, Long categoryId, String status) {
        return Product.of(id, shopId, categoryId, "煎饼果子", "现做现卖",
                new BigDecimal("8.50"), 20, status);
    }

    private static Category category(Long id, Long shopId) {
        return Category.of(id, shopId, "主食", 1);
    }

    private static Shop shop(Long merchantId, Long id) {
        Shop shop = Shop.initiallyClosed(merchantId, "北洋餐厅");
        shop.setId(id);
        return shop;
    }

    private static void assertCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo(code);
    }
}
