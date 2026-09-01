package cn.edu.tju.takeout.product;

import cn.edu.tju.takeout.catalog.CategoryMapper;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    public ProductService(
            ProductMapper productMapper, CategoryMapper categoryMapper, ShopMapper shopMapper) {
        // 仅保留依赖签名，等待功能开发人员实现。
    }

    public ProductView create(Long merchantId, Long shopId, ProductRequest request) {
        throw pending();
    }

    public ProductView update(Long merchantId, Long productId, ProductRequest request) {
        throw pending();
    }

    public ProductView changeStatus(
            Long merchantId, Long productId, ProductStatusRequest request) {
        throw pending();
    }

    public void delete(Long merchantId, Long productId) {
        throw pending();
    }

    public List<ProductView> listVisible(Long shopId, Long categoryId) {
        throw pending();
    }

    public ProductView updateStock(Long merchantId, Long productId, StockRequest request) {
        throw pending();
    }

    public void decreaseStock(Long productId, Integer quantity) {
        throw pending();
    }

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：商品业务尚未实现");
    }
}
