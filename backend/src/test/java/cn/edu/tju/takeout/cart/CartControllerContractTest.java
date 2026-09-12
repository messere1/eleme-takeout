package cn.edu.tju.takeout.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.auth.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartControllerContractTest {
    private final CartService service = mock(CartService.class);
    private final CartController controller = new CartController(service);
    private final UserPrincipal customer = new UserPrincipal(7L, "CUSTOMER");

    @Test
    void delegatesEveryCartEndpointWithAuthenticatedCustomer() {
        AddCartRequest add = new AddCartRequest(5L, 2);
        UpdateCartRequest update = new UpdateCartRequest(3);
        CartItemView item = new CartItemView(9L, 5L, 2);
        CartView cart = new CartView(List.of(), BigDecimal.ZERO);
        CartDeliveryRequest deliveryRequest = new CartDeliveryRequest(
                20L, "张同学", "13800138000", "天津大学北洋园校区", true);
        CartDeliveryInfo deliveryInfo = CartDeliveryInfo.of(
                7L, 20L, "张同学", "13800138000", "天津大学北洋园校区");
        when(service.add(7L, add)).thenReturn(item);
        when(service.get(7L)).thenReturn(cart);
        when(service.update(7L, 9L, update)).thenReturn(item);
        when(service.getDeliveryInfo(7L, 20L)).thenReturn(deliveryInfo);
        when(service.saveDeliveryInfo(7L, deliveryRequest)).thenReturn(deliveryInfo);

        assertThat(controller.add(customer, add).data()).isEqualTo(item);
        assertThat(controller.get(customer).data()).isEqualTo(cart);
        assertThat(controller.update(customer, 9L, update).data()).isEqualTo(item);
        assertThat(controller.getDeliveryInfo(customer, 20L).data()).isEqualTo(deliveryInfo);
        assertThat(controller.saveDeliveryInfo(customer, deliveryRequest).data()).isEqualTo(deliveryInfo);
        assertThat(controller.delete(customer, 9L).data()).isNull();
        assertThat(controller.clear(customer).data()).isNull();

        verify(service).delete(7L, 9L);
        verify(service).clear(7L);
        verify(service).getDeliveryInfo(7L, 20L);
        verify(service).saveDeliveryInfo(7L, deliveryRequest);
    }
}
