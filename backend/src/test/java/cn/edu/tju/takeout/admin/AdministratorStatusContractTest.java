package cn.edu.tju.takeout.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.order.OrderMapper;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.user.UserMapper;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PatchMapping;

class AdministratorStatusContractTest {
    @Test
    void exposesAdministratorStatusEndpointAndMapperGuards() throws Exception {
        Method endpoint = AdminController.class.getMethod(
                "administratorStatus", UserPrincipal.class, Long.class, AdminStatusRequest.class);
        assertThat(endpoint.getAnnotation(PatchMapping.class).value())
                .containsExactly("/administrators/{id}/status");
        assertThat(AdminMapper.class.getMethod("countEnabled")).isNotNull();
        assertThat(AdminMapper.class.getMethod("setEnabled", Long.class, boolean.class)).isNotNull();
    }

    @Test
    void cannotDisableTheLastEnabledAdministrator() {
        AdminMapper admins = mock(AdminMapper.class);
        Admin target = mock(Admin.class);
        when(target.isEnabled()).thenReturn(true);
        when(admins.findById(1L)).thenReturn(Optional.of(target));
        when(admins.countEnabled()).thenReturn(1L);
        AdminController controller = controller(admins);

        assertThatThrownBy(() -> controller.administratorStatus(
                new UserPrincipal(1L, "ADMIN"), 1L, new AdminStatusRequest("DISABLED")))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).code())
                .isEqualTo("BUSINESS_CONFLICT");
        verify(admins, never()).setEnabled(1L, false);
    }

    @Test
    void disablesAdministratorWhenAnotherEnabledAccountRemainsAndAuditsIt() {
        AdminMapper admins = mock(AdminMapper.class);
        Admin target = mock(Admin.class);
        when(target.isEnabled()).thenReturn(true);
        when(admins.findById(2L)).thenReturn(Optional.of(target));
        when(admins.countEnabled()).thenReturn(2L);
        when(admins.setEnabled(2L, false)).thenReturn(1);
        AdminController controller = controller(admins);

        controller.administratorStatus(
                new UserPrincipal(1L, "ADMIN"), 2L, new AdminStatusRequest("DISABLED"));

        verify(admins).setEnabled(2L, false);
        verify(admins).audit(1L, "SET_STATUS", "ADMINISTRATOR", 2L);
    }

    private AdminController controller(AdminMapper admins) {
        return new AdminController(mock(UserMapper.class), mock(MerchantMapper.class),
                mock(ProductMapper.class), mock(OrderMapper.class), admins);
    }
}
