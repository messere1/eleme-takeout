package cn.edu.tju.takeout.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

class PersistenceSecurityContractTest {

    @Test
    void mapperSqlUsesBoundParametersInsteadOfStringSubstitution() throws Exception {
        List<Class<?>> mappers = List.of(
                cn.edu.tju.takeout.cart.CartMapper.class,
                cn.edu.tju.takeout.catalog.CategoryMapper.class,
                cn.edu.tju.takeout.merchant.MerchantMapper.class,
                cn.edu.tju.takeout.order.OrderMapper.class,
                cn.edu.tju.takeout.product.ProductMapper.class,
                cn.edu.tju.takeout.shop.ShopMapper.class,
                cn.edu.tju.takeout.user.UserMapper.class);

        for (Class<?> mapper : mappers) {
            for (Method method : mapper.getDeclaredMethods()) {
                for (Annotation annotation : method.getDeclaredAnnotations()) {
                    String sql = sql(annotation);
                    if (sql != null) {
                        assertThat(sql)
                                .as("%s.%s must not use ${...}",
                                        mapper.getSimpleName(), method.getName())
                                .doesNotContain("${");
                    }
                }
            }
        }
    }

    private static String sql(Annotation annotation) throws Exception {
        if (!(annotation instanceof Select)
                && !(annotation instanceof Insert)
                && !(annotation instanceof Update)
                && !(annotation instanceof Delete)) {
            return null;
        }
        String[] lines = (String[]) annotation.annotationType()
                .getMethod("value")
                .invoke(annotation);
        return String.join("\n", Arrays.asList(lines));
    }
}
