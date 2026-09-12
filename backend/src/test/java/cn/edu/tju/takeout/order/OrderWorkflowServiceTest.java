package cn.edu.tju.takeout.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.cart.CartMapper;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.merchant.Merchant;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopMapper;
import cn.edu.tju.takeout.user.User;
import cn.edu.tju.takeout.user.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowServiceTest {
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
    void merchantListsOnlyOwnShopOrdersWithValidatedPagination() {
        Shop shop = shop(12L, 20L);
        Order order = order(60L, 7L, 20L, "CREATED");
        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.of(shop));
        when(orderMapper.findPageByShopId(20L, "CREATED", null, null, 5, 5))
                .thenReturn(List.of(order));
        when(orderMapper.countByShopId(20L, "CREATED", null, null)).thenReturn(6L);

        OrderPage result = service.listForMerchant(
                12L, new OrderQuery("CREATED", null, null, 2, 5));

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.items()).extracting(OrderSummaryView::orderNo).containsExactly("NO-60");
    }

    @Test
    void merchantWithoutShopCannotListOrders() {
        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.empty());
        assertCode(() -> service.listForMerchant(
                12L, new OrderQuery(null, null, null, 1, 20)), "FORBIDDEN");
    }

    @Test
    void merchantOrderListRejectsInvalidPageAndTimeRange() {
        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.of(shop(12L, 20L)));
        assertCode(() -> service.listForMerchant(
                12L, new OrderQuery(null, null, null, 1, 101)), "VALIDATION_ERROR");

        LocalDateTime start = LocalDateTime.of(2026, 9, 2, 0, 0);
        LocalDateTime end = start.minusDays(1);
        assertCode(() -> service.listForMerchant(
                12L, new OrderQuery(null, start, end, 1, 20)), "VALIDATION_ERROR");
    }

    @Test
    void ownerCancelsCreatedOrderAndRestoresStockOnce() {
        Order order = order(60L, 7L, 20L, "CREATED");
        OrderItem item = item(60L, 40L, 2);
        when(orderMapper.findById(60L)).thenReturn(Optional.of(order));
        when(orderMapper.markCancelledIfAllowed(60L)).thenReturn(1);
        when(orderMapper.findItemsByOrderId(60L)).thenReturn(List.of(item));

        OrderView result = service.cancel(7L, 60L);

        assertThat(result.status()).isEqualTo("CANCELLED");
        verify(productMapper).increaseStock(40L, 2);
    }

    @Test
    void cancelRejectsMissingForeignCancelledAndWrongStateOrders() {
        when(orderMapper.findById(1L)).thenReturn(Optional.empty());
        assertCode(() -> service.cancel(7L, 1L), "RESOURCE_NOT_FOUND");

        when(orderMapper.findById(2L)).thenReturn(Optional.of(order(2L, 8L, 20L, "CREATED")));
        assertCode(() -> service.cancel(7L, 2L), "FORBIDDEN");

        when(orderMapper.findById(3L)).thenReturn(Optional.of(order(3L, 7L, 20L, "CANCELLED")));
        assertCode(() -> service.cancel(7L, 3L), "ORDER_ALREADY_CANCELLED");

        when(orderMapper.findById(4L)).thenReturn(Optional.of(order(4L, 7L, 20L, "COMPLETED")));
        assertCode(() -> service.cancel(7L, 4L), "BUSINESS_CONFLICT");
    }

    @Test
    void concurrentCancelDetectsAlreadyCancelledState() {
        Order created = order(60L, 7L, 20L, "CREATED");
        Order cancelled = order(60L, 7L, 20L, "CANCELLED");
        when(orderMapper.findById(60L)).thenReturn(Optional.of(created), Optional.of(cancelled));
        when(orderMapper.markCancelledIfAllowed(60L)).thenReturn(0);

        assertCode(() -> service.cancel(7L, 60L), "ORDER_ALREADY_CANCELLED");
        verify(productMapper, never()).increaseStock(40L, 2);
    }

    @Test
    void concurrentCancelWithUnchangedStateReturnsConflict() {
        Order created = order(60L, 7L, 20L, "CREATED");
        when(orderMapper.findById(60L)).thenReturn(Optional.of(created));
        when(orderMapper.markCancelledIfAllowed(60L)).thenReturn(0);

        assertCode(() -> service.cancel(7L, 60L), "BUSINESS_CONFLICT");
    }

    @Test
    void merchantAcceptsOnlyOwnPaidCreatedOrder() {
        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.of(shop(12L, 20L)));
        when(orderMapper.findById(60L)).thenReturn(
                Optional.of(paidOrder(60L, 7L, 20L, "CREATED")),
                Optional.of(order(60L, 7L, 20L, "ACCEPTED")));
        when(orderMapper.transitionStatus(60L, "CREATED", "ACCEPTED")).thenReturn(1);
        when(orderMapper.findItemsByOrderId(60L)).thenReturn(List.of());

        assertThat(service.accept(12L, 60L).status()).isEqualTo("ACCEPTED");
    }

    @Test
    void acceptRejectsForeignOrderAndStatusConflict() {
        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.of(shop(12L, 20L)));
        when(orderMapper.findById(61L)).thenReturn(Optional.of(order(61L, 7L, 21L, "CREATED")));
        assertCode(() -> service.accept(12L, 61L), "FORBIDDEN");

        when(orderMapper.findById(62L)).thenReturn(Optional.of(order(62L, 7L, 20L, "CREATED")));
        when(orderMapper.transitionStatus(62L, "CREATED", "ACCEPTED")).thenReturn(0);
        assertCode(() -> service.accept(12L, 62L), "BUSINESS_CONFLICT");
    }

    @Test
    void merchantCannotAcceptAnUnpaidOrder() {
        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.of(shop(12L, 20L)));
        when(orderMapper.findById(60L)).thenReturn(Optional.of(order(60L, 7L, 20L, "CREATED")));

        assertCode(() -> service.accept(12L, 60L), "BUSINESS_CONFLICT");
        verify(orderMapper, never()).transitionStatus(60L, "CREATED", "ACCEPTED");
    }

    @Test
    void customerConfirmsOwnDeliveredOrder() {
        when(orderMapper.findById(60L)).thenReturn(
                Optional.of(order(60L, 7L, 20L, "DELIVERED")),
                Optional.of(order(60L, 7L, 20L, "COMPLETED")));
        when(orderMapper.transitionStatus(60L, "DELIVERED", "COMPLETED")).thenReturn(1);
        when(orderMapper.findItemsByOrderId(60L)).thenReturn(List.of());

        assertThat(service.confirmReceived(7L, 60L).status()).isEqualTo("COMPLETED");
    }

    @Test
    void confirmationRejectsForeignCustomerAndStatusConflict() {
        when(orderMapper.findById(61L)).thenReturn(Optional.of(order(61L, 8L, 20L, "ACCEPTED")));
        assertCode(() -> service.confirmReceived(7L, 61L), "FORBIDDEN");

        when(orderMapper.findById(62L)).thenReturn(Optional.of(order(62L, 7L, 20L, "ACCEPTED")));
        assertCode(() -> service.confirmReceived(7L, 62L), "BUSINESS_CONFLICT");
    }

    @Test
    void detailMasksCustomerPhoneAndAddressAndIncludesShopPhone() {
        Order order = order(60L, 7L, 20L, "CREATED");
        order.setAddress("天津市津南区海河教育园");
        User user = User.registered("user001", "13800138000", "hash");
        user.setId(7L);
        Merchant merchant = Merchant.registered("北洋餐厅", "13900139000", "hash", "快餐");
        merchant.setId(12L);
        when(orderMapper.findById(60L)).thenReturn(Optional.of(order));
        when(orderMapper.findItemsByOrderId(60L)).thenReturn(List.of());
        when(shopMapper.findById(20L)).thenReturn(Optional.of(shop(12L, 20L)));
        when(userMapper.findById(7L)).thenReturn(Optional.of(user));
        when(merchantMapper.findById(12L)).thenReturn(Optional.of(merchant));

        OrderView result = service.getDetail(7L, "CUSTOMER", 60L);

        assertThat(result.shopPhone()).isEqualTo("13900139000");
        assertThat(result.userPhoneMasked()).isEqualTo("138****8000");
        assertThat(result.userAddress()).isEqualTo("天津****育园");
    }

    @Test
    void missingResourcesAtWorkflowLookupsReturnNotFound() {
        Order created = order(70L, 7L, 20L, "CREATED");
        when(orderMapper.findById(70L)).thenReturn(Optional.of(created), Optional.empty());
        when(orderMapper.markCancelledIfAllowed(70L)).thenReturn(0);
        assertCode(() -> service.cancel(7L, 70L), "RESOURCE_NOT_FOUND");

        when(orderMapper.findById(71L)).thenReturn(Optional.of(order(71L, 7L, 20L, "CREATED")));
        when(shopMapper.findById(20L)).thenReturn(Optional.empty());
        assertCode(() -> service.getDetail(12L, "MERCHANT", 71L), "RESOURCE_NOT_FOUND");

        when(shopMapper.findByMerchantId(12L)).thenReturn(Optional.of(shop(12L, 20L)));
        when(orderMapper.findById(72L)).thenReturn(Optional.empty());
        assertCode(() -> service.accept(12L, 72L), "RESOURCE_NOT_FOUND");

        when(orderMapper.findById(73L)).thenReturn(
                Optional.of(order(73L, 7L, 20L, "CREATED")), Optional.empty());
        when(orderMapper.transitionStatus(73L, "CREATED", "ACCEPTED")).thenReturn(1);
        assertCode(() -> service.accept(12L, 73L), "RESOURCE_NOT_FOUND");

        when(orderMapper.findById(76L)).thenReturn(Optional.empty());
        assertCode(() -> service.confirmReceived(7L, 76L), "RESOURCE_NOT_FOUND");

        when(orderMapper.findById(77L)).thenReturn(Optional.of(order(77L, 7L, 20L, "DELIVERED")), Optional.empty());
        when(orderMapper.transitionStatus(77L, "DELIVERED", "COMPLETED")).thenReturn(1);
        assertCode(() -> service.confirmReceived(7L, 77L), "RESOURCE_NOT_FOUND");
    }

    private static Shop shop(Long merchantId, Long id) {
        Shop shop = Shop.initiallyClosed(merchantId, "北洋餐厅");
        shop.setId(id);
        return shop;
    }

    private static Order order(Long id, Long userId, Long shopId, String status) {
        return Order.restore(id, "NO-" + id, userId, shopId, new BigDecimal("17.00"),
                status, LocalDateTime.of(2026, 9, 8, 12, 0));
    }

    private static Order paidOrder(Long id, Long userId, Long shopId, String status) {
        Order order = order(id, userId, shopId, status);
        order.markPaid(LocalDateTime.of(2026, 9, 8, 12, 1));
        return order;
    }

    private static OrderItem item(Long orderId, Long productId, int quantity) {
        return OrderItem.restore(70L, orderId, productId, "煎饼果子",
                new BigDecimal("8.50"), quantity, new BigDecimal("17.00"));
    }

    private static void assertCode(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo(code);
    }
}
