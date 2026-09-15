package cn.edu.tju.takeout.admin;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.takeout.auth.UserPrincipal;
import java.lang.reflect.Method;
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
}
