package cn.edu.tju.takeout.cart;

import cn.edu.tju.takeout.product.ProductMapper;
import org.springframework.stereotype.Service;

@Service
public class CartService {
    public CartService(CartMapper cartMapper, ProductMapper productMapper) {
        // 仅保留依赖签名，等待功能开发人员实现。
    }

    public CartItemView add(Long userId, AddCartRequest request) {
        throw pending();
    }

    public CartView get(Long userId) {
        throw pending();
    }

    public CartItemView update(Long userId, Long itemId, UpdateCartRequest request) {
        throw pending();
    }

    public void delete(Long userId, Long itemId) {
        throw pending();
    }

    public void clear(Long userId) {
        throw pending();
    }

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：购物车业务尚未实现");
    }
}
