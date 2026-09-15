package cn.edu.tju.takeout.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.auth.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopControllerContractTest {
    private final ShopService service = mock(ShopService.class);
    private final ShopController controller = new ShopController(
            service,
            mock(cn.edu.tju.takeout.recommend.SearchHistoryService.class),
            mock(cn.edu.tju.takeout.recommend.RecommendationService.class));
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

    @Test
    void searchRecordsKeywordForLoggedInCustomer() {
        cn.edu.tju.takeout.recommend.SearchHistoryService history =
                mock(cn.edu.tju.takeout.recommend.SearchHistoryService.class);
        UserPrincipal customer = new UserPrincipal(7L, "CUSTOMER");
        when(service.searchShops(1, 20, "简餐")).thenReturn(new ShopPage(List.of(), 1, 20, 0, 0));

        controller(history).searchShops(customer, 1, 20, "简餐");

        verify(history).record(7L, "简餐");
    }

    /** 关键词必须在搜索成功后记录：否则页码/每页条数非法的请求也会污染搜索历史 */
    @Test
    void rejectedSearchDoesNotRecordKeyword() {
        cn.edu.tju.takeout.recommend.SearchHistoryService history =
                mock(cn.edu.tju.takeout.recommend.SearchHistoryService.class);
        UserPrincipal customer = new UserPrincipal(7L, "CUSTOMER");
        when(service.searchShops(any(), any(), any()))
                .thenThrow(new IllegalStateException("页码必须大于等于1"));

        assertThatThrownBy(() -> controller(history).searchShops(customer, 0, 20, "简餐"))
                .isInstanceOf(IllegalStateException.class);

        verify(history, never()).record(any(), any());
    }

    @Test
    void nonCustomerSearchDoesNotWriteAnotherUsersHistory() {
        cn.edu.tju.takeout.recommend.SearchHistoryService history =
                mock(cn.edu.tju.takeout.recommend.SearchHistoryService.class);
        when(service.searchShops(1, 20, "简餐"))
                .thenReturn(new ShopPage(List.of(), 1, 20, 0, 0));

        controller(history).searchShops(merchant, 1, 20, "简餐");

        verifyNoInteractions(history);
    }

    @Test
    void nonCustomerShopListDoesNotReadAnotherUsersPreferences() {
        cn.edu.tju.takeout.recommend.RecommendationService recommendations =
                mock(cn.edu.tju.takeout.recommend.RecommendationService.class);
        ShopPage page = new ShopPage(List.of(), 1, 20, 0, 0);
        when(service.listShops(1, 20, null, null)).thenReturn(page);
        when(recommendations.reorderByPreference(null, page)).thenReturn(page);
        ShopController subject = new ShopController(
                service, mock(cn.edu.tju.takeout.recommend.SearchHistoryService.class), recommendations);

        assertThat(subject.listShops(merchant, 1, 20, null, null).data()).isEqualTo(page);

        verify(recommendations).reorderByPreference(null, page);
    }

    private ShopController controller(cn.edu.tju.takeout.recommend.SearchHistoryService history) {
        return new ShopController(
                service, history, mock(cn.edu.tju.takeout.recommend.RecommendationService.class));
    }
}
