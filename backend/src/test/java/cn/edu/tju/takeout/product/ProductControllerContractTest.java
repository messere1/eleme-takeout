package cn.edu.tju.takeout.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.auth.UserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductControllerContractTest {
    private final ProductService service = mock(ProductService.class);
    private final ProductController controller = new ProductController(service);
    private final UserPrincipal merchant = new UserPrincipal(12L, "MERCHANT");

    @Test
    void delegatesEveryProductEndpointWithAuthenticatedMerchant() {
        ProductRequest request = new ProductRequest(
                "煎饼果子", 3L, "现做", new BigDecimal("9.90"), 20);
        ProductView view = new ProductView(
                5L, 20L, 3L, "煎饼果子", "现做", new BigDecimal("9.90"), 20, "ON_SALE");
        ProductStatusRequest status = new ProductStatusRequest("OFF_SALE");
        StockRequest stock = new StockRequest(15);
        when(service.listVisible(20L, 3L)).thenReturn(List.of(view));
        when(service.create(12L, 20L, request)).thenReturn(view);
        when(service.update(12L, 5L, request)).thenReturn(view);
        when(service.changeStatus(12L, 5L, status)).thenReturn(view);
        when(service.updateStock(12L, 5L, stock)).thenReturn(view);

        assertThat(controller.list(20L, 3L).data()).containsExactly(view);
        assertThat(controller.create(merchant, 20L, request).data()).isEqualTo(view);
        assertThat(controller.update(merchant, 5L, request).data()).isEqualTo(view);
        assertThat(controller.changeStatus(merchant, 5L, status).data()).isEqualTo(view);
        assertThat(controller.updateStock(merchant, 5L, stock).data()).isEqualTo(view);
        assertThat(controller.delete(merchant, 5L).data()).isNull();

        verify(service).delete(12L, 5L);
    }
}
