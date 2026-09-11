package cn.edu.tju.takeout.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @Test
    void forbiddenNotFoundAndConflictResponsesKeepTraceIdWithoutInternalDetails() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        for (String path : new String[] {"forbidden", "not-found", "conflict"}) {
            mvc.perform(get("/test/" + path))
                    .andExpect(jsonPath("$.traceId").isNotEmpty())
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("java."))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("SELECT "))));
        }
    }

    @Test
    void unexpectedExceptionHasUnifiedInternalErrorHandler() {
        assertThat(java.util.Arrays.stream(GlobalExceptionHandler.class.getDeclaredMethods()))
                .anySatisfy(method -> {
                    org.springframework.web.bind.annotation.ExceptionHandler annotation =
                            method.getAnnotation(
                                    org.springframework.web.bind.annotation.ExceptionHandler.class);
                    assertThat(annotation).isNotNull();
                    assertThat(annotation.value()).contains(Exception.class);
                });
    }

    @Test
    void unexpectedExceptionReturnsSafeTraceableInternalError() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("jdbc:postgresql"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("C:\\secret"))));
    }

    @RestController
    static class FailingController {
        @GetMapping("/test/failure")
        String fail() {
            throw new BusinessException(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", "资源状态冲突");
        }

        @GetMapping("/test/forbidden")
        String forbidden() {
            throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权访问");
        }

        @GetMapping("/test/not-found")
        String notFound() {
            throw new BusinessException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "资源不存在");
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new BusinessException(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", "状态冲突");
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException(
                    "jdbc:postgresql://db/orders at C:\\secret\\application.yml");
        }
    }
}
