package cn.edu.tju.takeout.cart;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.Product;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.user.UserMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CartService {
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    public CartService(CartMapper cartMapper, ProductMapper productMapper) {
        this(cartMapper, productMapper, null);
    }
    @Autowired
    public CartService(CartMapper cartMapper, ProductMapper productMapper, UserMapper userMapper) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public CartItemView add(Long userId, AddCartRequest request) {
        Product product = productMapper
                .findById(request.productId())
                .orElseThrow(() ->
                        notFound("商品不存在"));

        if (!"ON_SALE".equals(
                product.getStatus())) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BUSINESS_CONFLICT",
                    "商品已下架"
            );
        }

        CartItem item = cartMapper
                .findByUserAndProduct(
                        userId,
                        request.productId()
                )
                .orElse(null);

        int quantity = request.quantity();

        if (item != null) {
            quantity += item.getQuantity();
        }

        if (quantity > product.getStock()) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BUSINESS_CONFLICT",
                    "商品库存不足"
            );
        }

        if (item == null) {

            item = CartItem.of(
                    null,
                    userId,
                    request.productId(),
                    quantity
            );

            cartMapper.insert(item);

        } else {

            item.changeQuantity(quantity);

            cartMapper.updateQuantity(item);
        }

        return CartItemView.from(item);
    }

    public CartView get(Long userId) {
        List<CartLineView> items =
                cartMapper
                        .findDetailsByUserId(userId)
                        .stream()
                        .map(CartLineView::from)
                        .toList();

        BigDecimal totalAmount =
                items.stream()
                        .filter(CartLineView::available)
                        .map(CartLineView::subtotal)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return new CartView(
                items,
                totalAmount
        );
    }
    @Transactional
    public CartItemView update(Long userId, Long itemId, UpdateCartRequest request) {
        CartItem item =
                cartMapper
                        .findByIdAndUserId(
                                itemId,
                                userId
                        )
                        .orElseThrow(() ->
                                notFound("购物车条目不存在"));

        if (request.quantity() == 0) {

            cartMapper.deleteByIdAndUserId(
                    itemId,
                    userId
            );

            return new CartItemView(
                    item.getId(),
                    item.getProductId(),
                    0
            );
        }

        Product product =
                productMapper
                        .findById(
                                item.getProductId()
                        )
                        .orElseThrow(() ->
                                notFound("商品不存在"));

        if (!"ON_SALE".equals(
                product.getStatus())) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BUSINESS_CONFLICT",
                    "商品已下架"
            );
        }

        if (request.quantity()
                > product.getStock()) {

            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BUSINESS_CONFLICT",
                    "商品库存不足"
            );
        }

        item.changeQuantity(
                request.quantity()
        );

        cartMapper.updateQuantity(item);

        return CartItemView.from(item);
    }

    @Transactional
    public void delete(Long userId, Long itemId) {
        int affected =
                cartMapper.deleteByIdAndUserId(
                        itemId,
                        userId
                );

        if (affected == 0) {

            throw notFound(
                    "购物车条目不存在"
            );
        }
    }

    @Transactional
    public void clear(Long userId) {
        cartMapper.deleteByUserId(userId);
    }

    public CartDeliveryInfo getDeliveryInfo(Long userId, Long shopId) {
        return cartMapper.findDeliveryInfo(userId, shopId).orElse(null);
    }

    @Transactional
    public CartDeliveryInfo saveDeliveryInfo(Long userId, CartDeliveryRequest request) {
        String normalizedPhone = normalizePhone(request.recipientPhone());
        CartDeliveryInfo info = CartDeliveryInfo.of(userId, request.shopId(),
                request.recipientName().trim(), normalizedPhone,
                request.deliveryAddress().trim());
        if (cartMapper.findDeliveryInfo(userId, request.shopId()).isPresent()) cartMapper.updateDeliveryInfo(info);
        else cartMapper.insertDeliveryInfo(info);
        if (Boolean.TRUE.equals(request.saveToProfile()) && userMapper != null) {
            if (userMapper.updateAddress(userId, info.getDeliveryAddress()) == 0) {
                throw notFound("顾客不存在");
            }
        }
        return info;
    }

    private String normalizePhone(String phone) {
        String normalized = phone.replaceAll("[\\s-]", "");
        if (normalized.startsWith("+")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("\\d{7,15}")) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "联系电话格式不正确"
            );
        }
        return normalized;
    }

    private BusinessException notFound(
            String message) {

        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                message
        );
    }
}
