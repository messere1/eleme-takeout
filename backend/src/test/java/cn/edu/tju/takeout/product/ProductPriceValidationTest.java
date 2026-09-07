package cn.edu.tju.takeout.product;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductPriceValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void zeroPriceIsRejected() {
        ProductRequest request = request("0.00");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("price"));
    }

    @Test
    void priceWithMoreThanTwoDecimalPlacesIsRejected() {
        ProductRequest request = request("8.501");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("price"));
    }

    @Test
    void validPriceKeepsExactDecimalValue() {
        ProductRequest request = request("8.50");

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.price()).isEqualByComparingTo("8.50");
    }

    @Test
    void largestDecimalTenTwoValueIsAccepted() {
        ProductRequest request = request("99999999.99");

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.price()).isEqualByComparingTo("99999999.99");
    }

    @Test
    void valueOutsideDecimalTenTwoRangeIsRejectedWithoutTruncation() {
        ProductRequest request = request("100000000.00");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("price");
        assertThat(request.price()).isEqualByComparingTo("100000000.00");
    }

    private static ProductRequest request(String price) {
        return new ProductRequest("煎饼果子", 30L, "现做现卖", new BigDecimal(price), 20);
    }
}
