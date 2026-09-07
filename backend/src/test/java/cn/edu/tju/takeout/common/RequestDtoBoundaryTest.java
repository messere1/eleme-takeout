package cn.edu.tju.takeout.common;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.takeout.auth.LoginRequest;
import cn.edu.tju.takeout.cart.AddCartRequest;
import cn.edu.tju.takeout.cart.UpdateCartRequest;
import cn.edu.tju.takeout.catalog.CategoryRequest;
import cn.edu.tju.takeout.merchant.MerchantRegistrationRequest;
import cn.edu.tju.takeout.product.ProductRequest;
import cn.edu.tju.takeout.product.ProductStatusRequest;
import cn.edu.tju.takeout.product.StockRequest;
import cn.edu.tju.takeout.shop.ShopStatusRequest;
import cn.edu.tju.takeout.shop.ShopUpdateRequest;
import cn.edu.tju.takeout.user.UserProfileUpdateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RequestDtoBoundaryTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void loginRejectsBlankCredentialsAndUnknownRole() {
        assertThat(invalidFields(new LoginRequest("", "", "ADMIN")))
                .containsExactlyInAnyOrder("account", "password", "role");
    }

    @Test
    void merchantRegistrationRejectsEveryMalformedField() {
        assertThat(invalidFields(new MerchantRegistrationRequest("", "123", "plain", "")))
                .containsExactlyInAnyOrder("merchantName", "phone", "password", "businessScope");
    }

    @Test
    void profileAndShopUpdatesRejectBlankContent() {
        assertThat(invalidFields(new UserProfileUpdateRequest("", "123", "")))
                .containsExactlyInAnyOrder("nickname", "phone", "address");
        assertThat(invalidFields(new ShopUpdateRequest("", "")))
                .containsExactlyInAnyOrder("shopName", "notice");
    }

    @Test
    void statusRequestsRejectUnsupportedValues() {
        assertThat(invalidFields(new ShopStatusRequest("PAUSED"))).containsExactly("status");
        assertThat(invalidFields(new ProductStatusRequest("DELETED"))).containsExactly("status");
    }

    @Test
    void categoryRejectsBlankNameAndNegativeSort() {
        assertThat(invalidFields(new CategoryRequest("", -1)))
                .containsExactlyInAnyOrder("name", "sort");
    }

    @Test
    void productRejectsMissingCategoryInvalidPriceAndNegativeStock() {
        ProductRequest request = new ProductRequest(
                "商品", null, null, new BigDecimal("0.001"), -1);

        assertThat(invalidFields(request))
                .containsExactlyInAnyOrder("categoryId", "price", "stock");
    }

    @Test
    void stockRejectsNullAndNegativeValues() {
        assertThat(invalidFields(new StockRequest(null))).containsExactly("stock");
        assertThat(invalidFields(new StockRequest(-1))).containsExactly("stock");
    }

    @Test
    void cartRequestsEnforcePositiveAddAndNonNegativeUpdateQuantities() {
        assertThat(invalidFields(new AddCartRequest(null, 0)))
                .containsExactlyInAnyOrder("productId", "quantity");
        assertThat(invalidFields(new UpdateCartRequest(-1))).containsExactly("quantity");
        assertThat(invalidFields(new UpdateCartRequest(0))).isEmpty();
    }

    private Set<String> invalidFields(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
