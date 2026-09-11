package cn.edu.tju.takeout.order;

import cn.edu.tju.takeout.cart.CartCheckoutLine;
import cn.edu.tju.takeout.cart.CartMapper;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import cn.edu.tju.takeout.user.User;
import cn.edu.tju.takeout.user.UserMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDateTime;


@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    public OrderService(
            OrderMapper orderMapper, CartMapper cartMapper,
            ProductMapper productMapper, ShopMapper shopMapper,
            UserMapper userMapper, MerchantMapper merchantMapper) {
        this.orderMapper = orderMapper;
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
    }

    @Transactional
    public OrderView create(Long userId, String address) {
        User user = userMapper.findById(userId).orElse(null);
        return create(userId, new CreateOrderRequest(null,
                user != null ? user.getNickname() : "收货人",
                user != null ? user.getPhone() : "0000000",
                address != null ? address : (user != null ? user.getAddress() : "未填写地址"), false));
    }

    @Transactional
    public OrderView create(Long userId, CreateOrderRequest request) {
        List<CartCheckoutLine> lines =
                cartMapper
                        .findCheckoutLinesByUserId(
                                userId
                        );

        if (lines.isEmpty()) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BUSINESS_CONFLICT",
                    "购物车为空"
            );
        }

        Long shopId = request.shopId() == null ? lines.get(0).getShopId() : request.shopId();
        if (request.shopId() != null) {
            final Long selectedShopId = shopId;
            lines = lines.stream().filter(line -> selectedShopId.equals(line.getShopId())).toList();
            if (lines.isEmpty()) throw businessConflict("所选店铺购物车为空");
        }

        for (CartCheckoutLine line : lines) {

            if (!shopId.equals(
                    line.getShopId())) {

                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "BUSINESS_CONFLICT",
                        "一次订单只能包含同一家店铺的商品"
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

        for (CartCheckoutLine line : lines) {
            if (!"ON_SALE".equals(line.getStatus())) {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "BUSINESS_CONFLICT",
                        "购物车中存在已下架商品"
                );
            }
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
        totalAmount = totalAmount.setScale(2, java.math.RoundingMode.HALF_UP);

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
                Order.created(
                        generateOrderNo(),
                        userId,
                        shopId,
                        totalAmount
                );
        order.setRecipient(request.recipientName().trim(), request.recipientPhone().trim(),
                request.deliveryAddress().trim());
        if (Boolean.TRUE.equals(request.saveToProfile())) {
            userMapper.updateAddress(userId, request.deliveryAddress().trim());
        }

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

        if (request.shopId() == null) cartMapper.deleteByUserId(userId);
        else for (CartCheckoutLine line : lines) cartMapper.findByUserAndProduct(userId, line.getProductId())
                .ifPresent(item -> cartMapper.deleteByIdAndUserId(item.getId(), userId));

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
                        ? 20
                        : query.size();

        if (page < 1 || size < 1 || size > 100) {

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

        int offset = (page - 1) * size;

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

    public OrderPage listForMerchant(Long merchantId, OrderQuery query) {
        Shop shop = shopMapper.findByMerchantId(merchantId).orElseThrow(this::forbidden);
        PageRequest pageRequest = validatePage(query);
        List<Order> orders = orderMapper.findPageByShopId(
                shop.getId(), query.status(), query.startTime(), query.endTime(),
                pageRequest.size(), pageRequest.offset());
        long total = orderMapper.countByShopId(
                shop.getId(), query.status(), query.startTime(), query.endTime());
        return pageOf(orders, pageRequest.page(), pageRequest.size(), total);
    }

    @Transactional
    public OrderView cancel(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw forbidden();
        }
        if ("CANCELLED".equals(order.getStatus())) {
            throw alreadyCancelled();
        }
        if (!"CREATED".equals(order.getStatus())) {
            throw businessConflict("订单当前状态不可取消");
        }
        if (orderMapper.markCancelledIfAllowed(orderId) == 0) {
            Order current = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
            if ("CANCELLED".equals(current.getStatus())) {
                throw alreadyCancelled();
            }
            throw businessConflict("订单当前状态不可取消");
        }
        List<OrderItem> items = orderMapper.findItemsByOrderId(orderId);
        for (OrderItem item : items) {
            productMapper.increaseStock(item.getProductId(), item.getQuantity());
        }
        order.changeStatus("CANCELLED");
        return OrderView.from(order, items);
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

        Shop shopForPhone =
                shopMapper.findById(order.getShopId()).orElse(null);
        User buyer =
                userMapper.findById(order.getUserId()).orElse(null);
        final String[] shopPhoneHolder = new String[] { null };
        if (shopForPhone != null && shopForPhone.getMerchantId() != null) {
            merchantMapper.findById(shopForPhone.getMerchantId())
                    .ifPresent(merchant -> shopPhoneHolder[0] = merchant.getPhone());
        }
        String deliveryAddress = order.getAddress() != null
                ? order.getAddress()
                : (buyer != null ? buyer.getAddress() : null);
        return OrderView.from(order, items)
                .withContacts(
                        shopPhoneHolder[0],
                        maskPhone(buyer != null ? buyer.getPhone() : null),
                        maskAddress(deliveryAddress));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskAddress(String address) {
        if (address == null || address.length() <= 4) {
            return address;
        }
        return address.substring(0, 2) + "****" + address.substring(address.length() - 2);
    }

    @Transactional
    public OrderView accept(Long merchantId, Long orderId) {
        Shop shop = shopMapper.findByMerchantId(merchantId).orElseThrow(this::forbidden);
        Order order = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        if (!shop.getId().equals(order.getShopId())) {
            throw forbidden();
        }
        if (orderMapper.transitionStatus(orderId, "CREATED", "ACCEPTED") == 0) {
            throw businessConflict("订单状态不可接单");
        }
        Order updated = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        return OrderView.from(updated, orderMapper.findItemsByOrderId(orderId));
    }

    @Transactional
    public OrderView complete(Long merchantId, Long orderId) {
        Shop shop = shopMapper.findByMerchantId(merchantId).orElseThrow(this::forbidden);
        Order order = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        if (!shop.getId().equals(order.getShopId())) {
            throw forbidden();
        }
        if (orderMapper.transitionStatus(orderId, "ACCEPTED", "COMPLETED") == 0) {
            throw businessConflict("订单状态不可完成");
        }
        Order updated = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        return OrderView.from(updated, orderMapper.findItemsByOrderId(orderId));
    }

    @Transactional
    public OrderView confirmReceived(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        if (!order.getUserId().equals(userId)) {
            throw forbidden();
        }
        if (orderMapper.transitionStatus(orderId, "ACCEPTED", "COMPLETED") == 0
                && orderMapper.transitionStatus(orderId, "DELIVERED", "COMPLETED") == 0) {
            throw businessConflict("订单状态不可确认收货");
        }
        Order updated = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        return OrderView.from(updated, orderMapper.findItemsByOrderId(orderId));
    }

    @Transactional
    public OrderView pay(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        if (!order.getUserId().equals(userId)) throw forbidden();
        if ("PAID".equals(order.getPaymentStatus())) {
            return OrderView.from(order, orderMapper.findItemsByOrderId(orderId));
        }
        LocalDateTime now = LocalDateTime.now();
        if (orderMapper.markPaid(orderId, userId, now) == 0) {
            cancelExpiredOrders();
            throw businessConflict("订单已超时或当前状态不可支付");
        }
        Order updated = orderMapper.findById(orderId).orElseThrow(() -> notFound("订单不存在"));
        return OrderView.from(updated, orderMapper.findItemsByOrderId(orderId));
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        for (Order order : orderMapper.findExpiredUnpaid(now)) {
            if (orderMapper.cancelExpired(order.getId(), now) == 1) {
                for (OrderItem item : orderMapper.findItemsByOrderId(order.getId())) {
                    productMapper.increaseStock(item.getProductId(), item.getQuantity());
                }
            }
        }
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

    private PageRequest validatePage(OrderQuery query) {
        int page = query.page() == null ? 1 : query.page();
        int size = query.size() == null ? 20 : query.size();
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "分页参数不合法");
        }
        if (query.startTime() != null && query.endTime() != null
                && query.startTime().isAfter(query.endTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "开始时间不能晚于结束时间");
        }
        return new PageRequest(page, size, (page - 1) * size);
    }

    private OrderPage pageOf(List<Order> orders, int page, int size, long total) {
        int totalPages = total == 0 ? 0 : (int) ((total + size - 1) / size);
        return new OrderPage(orders.stream().map(OrderSummaryView::from).toList(),
                page, size, total, totalPages);
    }

    private BusinessException alreadyCancelled() {
        return new BusinessException(HttpStatus.CONFLICT, "ORDER_ALREADY_CANCELLED", "订单已取消");
    }

    private BusinessException businessConflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", message);
    }

    private record PageRequest(int page, int size, int offset) {}
}
