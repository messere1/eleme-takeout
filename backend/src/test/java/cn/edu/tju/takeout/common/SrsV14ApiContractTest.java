package cn.edu.tju.takeout.common;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.takeout.order.CreateOrderRequest;
import cn.edu.tju.takeout.order.Order;
import cn.edu.tju.takeout.order.OrderController;
import cn.edu.tju.takeout.order.OrderView;
import cn.edu.tju.takeout.cart.CartLineView;
import cn.edu.tju.takeout.merchant.MerchantController;
import cn.edu.tju.takeout.merchant.MerchantRegistrationRequest;
import cn.edu.tju.takeout.user.UserController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** SRS V2.0 第二阶段新增与变更需求的接口红灯基线。文件名保留以兼容历史链接。 */
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
                .as("FR-016/024 下单请求必须选择店铺购物车并包含完整收货信息")
                .contains("shopId", "recipientName", "recipientPhone", "deliveryAddress");
    }

    @Test
    void exposesAdministratorAccountLists() {
        Stream<Mapping> adminMappings = Stream.of(UserController.class, MerchantController.class)
                .flatMap(SrsV14ApiContractTest::allMappings)
                .filter(mapping -> mapping.path().startsWith("/api/v1/admin/"));

        assertThat(adminMappings)
                .extracting(Mapping::path)
                .contains("/api/v1/admin/users", "/api/v1/admin/merchants");
    }

    @Test
    void orderModelAndResponseCarryImmutableRecipientSnapshot() {
        assertThat(Arrays.stream(Order.class.getDeclaredFields()).map(field -> field.getName()))
                .contains("recipientName", "recipientPhone", "deliveryAddress");
        assertThat(Arrays.stream(OrderView.class.getRecordComponents())
                .map(component -> component.getName()))
                .contains("recipientName", "recipientPhoneMasked", "deliveryAddress");
    }

    @Test
    void merchantRegistrationCarriesRequiredShopAddress() {
        assertThat(Arrays.stream(MerchantRegistrationRequest.class.getRecordComponents())
                .map(component -> component.getName()))
                .contains("shopAddress");
    }

    @Test
    void cartLinesExposeShopIdentityForMultiShopIsolation() {
        assertThat(Arrays.stream(CartLineView.class.getRecordComponents())
                .map(component -> component.getName()))
                .contains("shopId");
    }

    @Test
    void exposesPaymentEndpoint() {
        assertThat(allMappings(OrderController.class).map(Mapping::path))
                .contains("/api/v1/orders/{orderId}/pay");
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
