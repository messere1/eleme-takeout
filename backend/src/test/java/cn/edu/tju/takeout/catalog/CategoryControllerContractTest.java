package cn.edu.tju.takeout.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.auth.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryControllerContractTest {
    private final CategoryService service = mock(CategoryService.class);
    private final CategoryController controller = new CategoryController(service);
    private final UserPrincipal merchant = new UserPrincipal(12L, "MERCHANT");

    @Test
    void delegatesEveryCategoryEndpointWithAuthenticatedMerchant() {
        CategoryRequest request = new CategoryRequest("热销", 1);
        CategoryView view = new CategoryView(3L, 20L, "热销", 1);
        when(service.list(20L)).thenReturn(List.of(view));
        when(service.create(12L, 20L, request)).thenReturn(view);
        when(service.update(12L, 3L, request)).thenReturn(view);

        assertThat(controller.list(20L).data()).containsExactly(view);
        assertThat(controller.create(merchant, 20L, request).data()).isEqualTo(view);
        assertThat(controller.update(merchant, 3L, request).data()).isEqualTo(view);
        assertThat(controller.delete(merchant, 3L).data()).isNull();

        verify(service).delete(12L, 3L);
    }
}
