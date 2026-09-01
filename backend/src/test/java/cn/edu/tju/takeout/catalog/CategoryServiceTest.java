package cn.edu.tju.takeout.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock private CategoryMapper categoryMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ShopMapper shopMapper;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryMapper, productMapper, shopMapper);
    }

    @Test
    void ownerCreatesCategoryWhenNameAndSortAreUnique() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop()));
        when(categoryMapper.findByName(20L, "主食")).thenReturn(Optional.empty());
        when(categoryMapper.findBySort(20L, 1)).thenReturn(Optional.empty());

        CategoryView result = categoryService.create(12L, 20L, new CategoryRequest("主食", 1));

        verify(categoryMapper).insert(org.mockito.ArgumentMatchers.any(Category.class));
        assertThat(result.name()).isEqualTo("主食");
        assertThat(result.sort()).isEqualTo(1);
    }

    @Test
    void duplicateSortReturnsConflict() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop()));
        when(categoryMapper.findByName(20L, "饮料")).thenReturn(Optional.empty());
        when(categoryMapper.findBySort(20L, 1)).thenReturn(Optional.of(new Category()));

        assertThatThrownBy(() -> categoryService.create(12L, 20L, new CategoryRequest("饮料", 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
    }

    @Test
    void publicListUsesStableSortThenIdOrder() {
        when(categoryMapper.findAllByShopId(20L)).thenReturn(List.of(
                Category.of(2L, 20L, "饮料", 2), Category.of(1L, 20L, "主食", 1)));

        assertThat(categoryService.list(20L)).extracting(CategoryView::id).containsExactly(2L, 1L);
        verify(categoryMapper).findAllByShopId(20L);
    }

    @Test
    void ownerUpdatesCategoryWhenNewValuesAreUnique() {
        Category category = Category.of(30L, 20L, "主食", 1);
        when(categoryMapper.findById(30L)).thenReturn(Optional.of(category));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop()));
        when(categoryMapper.findByName(20L, "招牌主食")).thenReturn(Optional.empty());
        when(categoryMapper.findBySort(20L, 2)).thenReturn(Optional.empty());

        CategoryView result = categoryService.update(12L, 30L, new CategoryRequest("招牌主食", 2));

        verify(categoryMapper).update(category);
        assertThat(result.name()).isEqualTo("招牌主食");
        assertThat(result.sort()).isEqualTo(2);
    }

    @Test
    void nonEmptyCategoryCannotBeDeleted() {
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop()));
        when(categoryMapper.findById(30L)).thenReturn(Optional.of(Category.of(30L, 20L, "主食", 1)));
        when(productMapper.countByCategoryId(30L)).thenReturn(2);

        assertThatThrownBy(() -> categoryService.delete(12L, 30L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
    }

    @Test
    void ownerDeletesEmptyCategory() {
        when(categoryMapper.findById(30L)).thenReturn(Optional.of(Category.of(30L, 20L, "主食", 1)));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop()));
        when(productMapper.countByCategoryId(30L)).thenReturn(0);

        categoryService.delete(12L, 30L);

        verify(categoryMapper).deleteById(30L);
    }

    private static Shop shop() {
        Shop shop = Shop.initiallyClosed(12L, "北洋餐厅");
        shop.setId(20L);
        return shop;
    }
}
