package cn.edu.tju.takeout.common;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.takeout.cart.CartController;
import cn.edu.tju.takeout.order.OrderController;
import cn.edu.tju.takeout.product.ProductController;
import cn.edu.tju.takeout.shop.ShopController;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

class SrsV12ApiMappingContractTest {

    @Test
    void exposesPublicPaginatedShopList() {
        assertMapping(ShopController.class, RequestMethod.GET, "/api/v1/shops");
    }

    @Test
    void exposesPaginatedProductsByCategoryAndProductDetail() {
        assertMapping(ProductController.class, RequestMethod.GET,
                "/api/v1/categories/{categoryId}/products");
        assertMapping(ProductController.class, RequestMethod.GET,
                "/api/v1/products/{productId}");
    }

    @Test
    void exposesDedicatedPriceUpdateEndpoint() {
        assertMapping(ProductController.class, RequestMethod.PATCH,
                "/api/v1/products/{productId}/price");
    }

    @Test
    void cartWriteMappingsFollowSrsV12() {
        assertMapping(CartController.class, RequestMethod.POST, "/api/v1/cart/items");
        assertMapping(CartController.class, RequestMethod.PATCH,
                "/api/v1/cart/items/{itemId}");
    }

    @Test
    void orderListDefaultsToTwentyItems() {
        Method list = Arrays.stream(OrderController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("list"))
                .findFirst()
                .orElseThrow();
        Parameter size = Arrays.stream(list.getParameters())
                .filter(parameter -> {
                    RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                    return requestParam != null && "size".equals(parameter.getName());
                })
                .findFirst()
                .orElseThrow();

        assertThat(size.getAnnotation(RequestParam.class).defaultValue()).isEqualTo("20");
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
        String base = firstPath(classMapping);
        return Arrays.stream(controller.getDeclaredMethods()).flatMap(method -> {
            RequestMapping mapping =
                    AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null || mapping.method().length == 0) {
                return Stream.empty();
            }
            String[] paths = paths(mapping);
            return Arrays.stream(paths).flatMap(path ->
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
