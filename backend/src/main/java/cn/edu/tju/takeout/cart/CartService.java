package cn.edu.tju.takeout.cart;

import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.product.Product;
import cn.edu.tju.takeout.product.ProductMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    public CartService(CartMapper cartMapper, ProductMapper productMapper) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
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

    private BusinessException notFound(
            String message) {

        return new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                message
        );
    }
}
