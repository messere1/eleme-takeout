package cn.edu.tju.takeout.catalog;

import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    public CategoryService(
            CategoryMapper categoryMapper, ProductMapper productMapper, ShopMapper shopMapper) {
        // 仅保留依赖签名，等待功能开发人员实现。
    }

    public CategoryView create(Long merchantId, Long shopId, CategoryRequest request) {
        throw pending();
    }

    public List<CategoryView> list(Long shopId) {
        throw pending();
    }

    public CategoryView update(Long merchantId, Long categoryId, CategoryRequest request) {
        throw pending();
    }

    public void delete(Long merchantId, Long categoryId) {
        throw pending();
    }

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：分类业务尚未实现");
    }
}
