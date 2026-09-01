package cn.edu.tju.takeout.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.auth.UserPrincipal;
import org.junit.jupiter.api.Test;

class ShopControllerContractTest {
    private final ShopService service = mock(ShopService.class);
    private final ShopController controller = new ShopController(service);
    private final UserPrincipal merchant = new UserPrincipal(12L, "MERCHANT");

    @Test
    void delegatesEveryShopEndpointWithAuthenticatedMerchant() {
        ShopView view = new ShopView(20L, 12L, "北洋餐厅", "欢迎光临", "OPEN");
        ShopStatusRequest status = new ShopStatusRequest("OPEN");
        ShopUpdateRequest update = new ShopUpdateRequest("北洋餐厅", "欢迎光临");
        when(service.changeStatus(12L, 20L, status)).thenReturn(view);
        when(service.getShop(20L)).thenReturn(view);
        when(service.updateShop(12L, 20L, update)).thenReturn(view);

        assertThat(controller.changeStatus(merchant, 20L, status).data()).isEqualTo(view);
        assertThat(controller.getShop(20L).data()).isEqualTo(view);
        assertThat(controller.updateShop(merchant, 20L, update).data()).isEqualTo(view);
    }
}
