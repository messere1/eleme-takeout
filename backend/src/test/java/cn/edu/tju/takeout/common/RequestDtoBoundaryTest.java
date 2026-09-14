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
import cn.edu.tju.takeout.user.UserRegistrationRequest;
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
    void loginAcceptsEveryBaselineRoleAndRejectsBlankCredentialsAndUnknownRole() {
        assertThat(invalidFields(new LoginRequest("", "", "UNKNOWN")))
                .containsExactlyInAnyOrder("account", "password", "role");
        assertThat(invalidFields(new LoginRequest("admin01", "Secret123", "ADMIN"))).isEmpty();
    }

    @Test
    void customerRegistrationEnforcesLengthPhoneAndPasswordComplexity() {
        assertThat(invalidFields(new UserRegistrationRequest("ab", "123", "abcdef")))
                .containsExactlyInAnyOrder("username", "phone", "password");
        assertThat(invalidFields(new UserRegistrationRequest(
                "u".repeat(31), "13800138000", "abc12345")))
                .containsExactly("username");
        assertThat(invalidFields(new UserRegistrationRequest(
                "valid_user", "13800138000", "123456")))
                .containsExactly("password");
    }

    @Test
    void profileRejectsMalformedPhoneAndOversizedFields() {
        assertThat(invalidFields(new UserProfileUpdateRequest(
                "n".repeat(31), "123", "a".repeat(256))))
                .containsExactlyInAnyOrder("nickname", "phone", "address");
    }

    @Test
    void merchantRegistrationRejectsOversizedText() {
        assertThat(invalidFields(new MerchantRegistrationRequest(
                "m".repeat(51), "13800138000", "abc12345", "s".repeat(101))))
                .containsExactlyInAnyOrder("merchantName", "businessScope");
    }

    @Test
    void merchantRegistrationRejectsEveryMalformedField() {
        assertThat(invalidFields(new MerchantRegistrationRequest("", "123", "plain", "")))
                .containsExactlyInAnyOrder("merchantName", "phone", "password", "businessScope");
    }

    @Test
    void customerRegistrationRejectsControlCharactersInIdentityFields() {
        assertThat(invalidFields(new UserRegistrationRequest(
                "user\n001", "13800138000", "Secret123")))
                .contains("username");
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
