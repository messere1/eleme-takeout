package cn.edu.tju.takeout.order;

import cn.edu.tju.takeout.cart.CartCheckoutLine;
import cn.edu.tju.takeout.cart.CartMapper;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    public OrderService(
            OrderMapper orderMapper, CartMapper cartMapper,
            ProductMapper productMapper, ShopMapper shopMapper) {
        this.orderMapper = orderMapper;
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
    }

    @Transactional
    public OrderView create(Long userId) {
        List<CartCheckoutLine> lines =
                cartMapper
                        .findCheckoutLinesByUserId(
                                userId
                        );

        if (lines.isEmpty()) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CART_EMPTY",
                    "购物车为空"
            );
        }

        Long shopId =
                lines.get(0).getShopId();

        for (CartCheckoutLine line : lines) {

            if (!shopId.equals(
                    line.getShopId())) {

                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "BUSINESS_CONFLICT",
                        "一次订单只能包含同一家店铺的商品"
                );
            }

            if (!"ON_SALE".equals(
                    line.getStatus())) {

                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "PRODUCT_OFF_SALE",
                        "购物车中存在已下架商品"
                );
            }
        }

        Shop shop =
                shopMapper
                        .findById(shopId)
                        .orElseThrow(() ->
                                notFound("店铺不存在"));

        if (!"OPEN".equals(
                shop.getStatus())) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BUSINESS_CONFLICT",
                    "店铺未营业"
            );
        }

        BigDecimal totalAmount =
                BigDecimal.ZERO;

        for (CartCheckoutLine line : lines) {

            BigDecimal subtotal =
                    line.getPrice().multiply(
                            BigDecimal.valueOf(
                                    line.getQuantity()
                            )
                    );

            totalAmount =
                    totalAmount.add(subtotal);
        }

        for (CartCheckoutLine line : lines) {

            int affected =
                    productMapper
                            .decreaseStockIfAvailable(
                                    line.getProductId(),
                                    line.getQuantity()
                            );

            if (affected == 0) {

                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "BUSINESS_CONFLICT",
                        "商品库存不足"
                );
            }
        }

        Order order =
                Order.pending(
                        generateOrderNo(),
                        userId,
                        shopId,
                        totalAmount
                );

        orderMapper.insert(order);

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartCheckoutLine line : lines) {

            OrderItem item =
                    OrderItem.from(
                            order.getId(),
                            line
                    );

            orderMapper.insertItem(item);
            orderItems.add(item);
        }

        cartMapper.deleteByUserId(userId);

        return OrderView.from(
                order,
                orderItems
        );
    }

    public OrderPage list(Long userId, OrderQuery query) {
        int page =
                query.page() == null
                        ? 1
                        : query.page();

        int size =
                query.size() == null
                        ? 10
                        : query.size();

        if (page < 1 || size < 1) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "分页参数不合法"
            );
        }

        if (query.startTime() != null
                && query.endTime() != null
                && query.startTime()
                .isAfter(query.endTime())) {

            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "开始时间不能晚于结束时间"
            );
        }

        int offset =
                (page - 1) * size;

        List<Order> orders =
                orderMapper.findPageByUserId(
                        userId,
                        query.status(),
                        query.startTime(),
                        query.endTime(),
                        size,
                        offset
                );

        long total =
                orderMapper.countByUserId(
                        userId,
                        query.status(),
                        query.startTime(),
                        query.endTime()
                );

        List<OrderSummaryView> items =
                orders.stream()
                        .map(OrderSummaryView::from)
                        .toList();

        int totalPages =
                total == 0
                        ? 0
                        : (int) (
                                (total + size - 1)
                                / size
                        );

        return new OrderPage(
                items,
                page,
                size,
                total,
                totalPages
        );
    }

    public OrderView getDetail(Long actorId, String role, Long orderId) {
        Order order =
                orderMapper
                        .findById(orderId)
                        .orElseThrow(() ->
                                notFound("订单不存在"));

        if ("CUSTOMER".equals(role)) {

            if (!order.getUserId()
                    .equals(actorId)) {

                throw forbidden();
            }

        } else if ("MERCHANT".equals(role)) {

            Shop shop =
                    shopMapper
                            .findById(
                                    order.getShopId()
                            )
                            .orElseThrow(() ->
                                    notFound("店铺不存在"));

            if (!shop.getMerchantId()
                    .equals(actorId)) {

                throw forbidden();
            }

        } else {

            throw forbidden();
        }

        List<OrderItem> items =
                orderMapper.findItemsByOrderId(
                        orderId
                );

        return OrderView.from(
                order,
                items
        );
    }

    private String generateOrderNo() {

        return "ORDER"
                + System.currentTimeMillis()
                + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }

    private BusinessException notFound(
            String message) {

        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                message
        );
    }

    private BusinessException forbidden() {

        return new BusinessException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "无权访问该订单"
        );
    }
}
