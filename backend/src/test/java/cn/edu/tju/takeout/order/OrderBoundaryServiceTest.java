package cn.edu.tju.takeout.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.cart.CartCheckoutLine;
import cn.edu.tju.takeout.cart.CartMapper;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import cn.edu.tju.takeout.user.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class OrderBoundaryServiceTest {
    @Mock private OrderMapper orderMapper;
    @Mock private CartMapper cartMapper;
    @Mock private ProductMapper productMapper;
    @Mock private ShopMapper shopMapper;
    @Mock private UserMapper userMapper;
    @Mock private MerchantMapper merchantMapper;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(
                orderMapper, cartMapper, productMapper, shopMapper, userMapper, merchantMapper);
    }

    @Test
    void emptyCartCannotCreateOrder() {
        when(cartMapper.findCheckoutLinesByUserId(7L)).thenReturn(List.of());
        assertCode(() -> service.create(7L, "天津大学"), "BUSINESS_CONFLICT");
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void productsFromDifferentShopsCannotCreateOneOrder() {
        when(cartMapper.findCheckoutLinesByUserId(7L)).thenReturn(List.of(
                line(50L, 20L, "ON_SALE"), line(51L, 21L, "ON_SALE")));
        assertCode(() -> service.create(7L, "天津大学"), "BUSINESS_CONFLICT");
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void missingShopRejectsOrderBeforePersistence() {
        when(cartMapper.findCheckoutLinesByUserId(7L))
                .thenReturn(List.of(line(50L, 20L, "ON_SALE")));
        when(shopMapper.findById(20L)).thenReturn(Optional.empty());
        assertCode(() -> service.create(7L, "天津大学"), "RESOURCE_NOT_FOUND");
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void offSaleProductRejectsOrderBeforePersistence() {
        when(cartMapper.findCheckoutLinesByUserId(7L))
                .thenReturn(List.of(line(50L, 20L, "OFF_SALE")));
        when(shopMapper.findById(20L)).thenReturn(Optional.of(openShop()));
        assertCode(() -> service.create(7L, "天津大学"), "BUSINESS_CONFLICT");
        verify(orderMapper, never()).insert(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void pageMustStartAtOne(int page) {
        assertCode(
                () -> service.list(7L, new OrderQuery(null, null, null, page, 10)),
                "VALIDATION_ERROR");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void pageSizeMustStayBetweenOneAndOneHundred(int size) {
        assertCode(
                () -> service.list(7L, new OrderQuery(null, null, null, 1, size)),
                "VALIDATION_ERROR");
    }

    @Test
    void missingOrderDetailReturnsNotFound() {
        when(orderMapper.findById(60L)).thenReturn(Optional.empty());
        assertCode(() -> service.getDetail(7L, "CUSTOMER", 60L), "RESOURCE_NOT_FOUND");
    }

    @Test
    void unsupportedRoleCannotReadOrderDetail() {
        when(orderMapper.findById(60L)).thenReturn(Optional.of(order()));
        assertCode(() -> service.getDetail(7L, "ADMIN", 60L), "FORBIDDEN");
    }

    private static CartCheckoutLine line(Long itemId, Long shopId, String status) {
        return CartCheckoutLine.of(
                itemId, shopId, itemId - 10, "商品", new BigDecimal("8.50"), 1, 10, status);
    }

    private static Shop openShop() {
        Shop shop = Shop.initiallyClosed(12L, "北洋餐厅");
        shop.setId(20L);
        shop.changeStatus("OPEN");
        return shop;
    }

    private static Order order() {
        return Order.restore(
                60L, "T20260907001", 7L, 20L, new BigDecimal("8.50"),
                "PENDING", LocalDateTime.of(2026, 9, 7, 12, 0));
    }

    private static void assertCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo(code);
    }
}
