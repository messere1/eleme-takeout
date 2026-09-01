package cn.edu.tju.takeout.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ValidationErrorTest {

    @Test
    void invalidFieldsReturnStructuredReadableErrors() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new ValidationController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"", "phone":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.msg").value("请求参数校验失败"))
                .andExpect(jsonPath("$.data.fieldErrors.username").value("用户名不能为空"))
                .andExpect(jsonPath("$.data.fieldErrors.phone").value("手机号格式不正确"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    record ValidationRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone) {
    }

    @RestController
    static class ValidationController {
        @PostMapping("/test/validation")
        ApiResponse<String> validate(@Valid @RequestBody ValidationRequest request) {
            return ApiResponse.success("should not be reached");
        }
    }
}

