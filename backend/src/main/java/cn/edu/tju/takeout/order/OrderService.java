package cn.edu.tju.takeout.order;

import cn.edu.tju.takeout.cart.CartMapper;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.ShopMapper;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    public OrderService(
            OrderMapper orderMapper, CartMapper cartMapper,
            ProductMapper productMapper, ShopMapper shopMapper) {
        // 仅保留依赖签名，等待功能开发人员实现。
    }

    public OrderView create(Long userId) {
        throw pending();
    }

    public OrderPage list(Long userId, OrderQuery query) {
        throw pending();
    }

    public OrderView getDetail(Long actorId, String role, Long orderId) {
        throw pending();
    }

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：订单业务尚未实现");
    }
}
