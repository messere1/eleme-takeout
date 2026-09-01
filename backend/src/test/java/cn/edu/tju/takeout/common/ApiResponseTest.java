package cn.edu.tju.takeout.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiResponseTest {

    @Test
    void successResponseContainsStableEnvelopeAndTraceId() {
        ApiResponse<String> response = ApiResponse.success("ready");

        assertThat(response.code()).isEqualTo(0);
        assertThat(response.msg()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ready");
        assertThat(response.traceId()).isNotBlank();
    }

    @Test
    void businessExceptionIsRenderedAsReadableStableError() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/test/failure"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_CONFLICT"))
                .andExpect(jsonPath("$.msg").value("资源状态冲突"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @RestController
    static class FailingController {
        @GetMapping("/test/failure")
        String fail() {
            throw new BusinessException(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", "资源状态冲突");
        }
    }
}
