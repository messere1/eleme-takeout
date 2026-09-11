package cn.edu.tju.takeout.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.takeout.common.ApiResponse;
import cn.edu.tju.takeout.common.GlobalExceptionHandler;
import jakarta.validation.Valid;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 从原始 JSON 层验证价格格式，防止反序列化后丢失输入表示信息。 */
class ProductPriceJsonContractTest {

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new PriceProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @ParameterizedTest
    @ValueSource(strings = {"1e2", "1E+2", "8.501", "0", "-1", "100000000.00"})
    void rejectsScientificNotationPrecisionAndRangeViolations(String price) throws Exception {
        mvc.perform(patch("/test/product-price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":" + price + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"not-a-number\""})
    void rejectsMissingOrNonNumericPrice(String price) throws Exception {
        mvc.perform(patch("/test/product-price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":" + price + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @RestController
    static class PriceProbeController {
        @PatchMapping("/test/product-price")
        ApiResponse<Void> update(@Valid @RequestBody ProductPriceRequest request) {
            return ApiResponse.success(null);
        }
    }
}
