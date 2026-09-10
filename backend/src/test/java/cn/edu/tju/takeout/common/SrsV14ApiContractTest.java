package cn.edu.tju.takeout.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import cn.edu.tju.takeout.order.CreateOrderRequest;
import cn.edu.tju.takeout.order.OrderController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** SRS V1.4 新增与变更需求的接口红灯基线，不包含业务实现。 */
class SrsV14ApiContractTest {

    @Test
    void exposesOrderAcceptCompleteAndCustomerConfirmationActions() {
        assertMapping(OrderController.class, RequestMethod.POST,
                "/api/v1/orders/{orderId}/accept");
        assertMapping(OrderController.class, RequestMethod.POST,
                "/api/v1/orders/{orderId}/complete");
        assertMapping(OrderController.class, RequestMethod.POST,
                "/api/v1/orders/{orderId}/confirm");
    }

    @Test
    void createOrderRequestCarriesCompleteRecipientSnapshot() {
        Set<String> fields = Arrays.stream(CreateOrderRequest.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());

        assertThat(fields)
                .as("FR-024 下单请求必须包含收货人、联系电话和收货地址")
                .contains("recipientName", "recipientPhone", "deliveryAddress");
    }

    @Test
    void exposesReadOnlyAdministratorAccountLists() {
        Class<?> controller = loadAdminController();

        assertMapping(controller, RequestMethod.GET, "/api/v1/admin/users");
        assertMapping(controller, RequestMethod.GET, "/api/v1/admin/merchants");
        assertThat(allMappings(controller))
                .as("FR-023 管理员第一阶段只能查询，不得提供写接口")
                .allSatisfy(mapping -> assertThat(mapping.httpMethod()).isEqualTo(RequestMethod.GET));
    }

    private static Class<?> loadAdminController() {
        try {
            return Class.forName("cn.edu.tju.takeout.admin.AdminController");
        } catch (ClassNotFoundException exception) {
            return fail("FR-023 缺少管理员只读接口：cn.edu.tju.takeout.admin.AdminController");
        }
    }

    private static void assertMapping(
            Class<?> controller, RequestMethod method, String expectedPath) {
        assertThat(allMappings(controller))
                .as("%s %s", method, expectedPath)
                .anySatisfy(mapping -> {
                    assertThat(mapping.httpMethod()).isEqualTo(method);
                    assertThat(mapping.path()).isEqualTo(expectedPath);
                });
    }

    private static Stream<Mapping> allMappings(Class<?> controller) {
        RequestMapping classMapping = controller.getAnnotation(RequestMapping.class);
        String base = classMapping == null ? "" : firstPath(classMapping);
        return Arrays.stream(controller.getDeclaredMethods()).flatMap(method -> {
            RequestMapping mapping =
                    AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null || mapping.method().length == 0) {
                return Stream.empty();
            }
            return Arrays.stream(paths(mapping)).flatMap(path ->
                    Arrays.stream(mapping.method())
                            .map(httpMethod -> new Mapping(httpMethod, base + path)));
        });
    }

    private static String firstPath(RequestMapping mapping) {
        String[] paths = paths(mapping);
        return paths.length == 0 ? "" : paths[0];
    }

    private static String[] paths(RequestMapping mapping) {
        if (mapping.path().length > 0) {
            return mapping.path();
        }
        if (mapping.value().length > 0) {
            return mapping.value();
        }
        return new String[] {""};
    }

    private record Mapping(RequestMethod httpMethod, String path) {}
}
