package cn.edu.tju.takeout.shop;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ShopStatusControllerTest {
    @Test
    void merchantCanChangeOwnedShopStatus() throws Exception {
        ShopService service = org.mockito.Mockito.mock(ShopService.class);
        when(service.changeStatus(eq(12L), eq(20L), any()))
                .thenReturn(new ShopView(20L, 12L, "北洋餐厅", null, "OPEN"));
        MockMvc mvc = mvc(service);

        mvc.perform(patch("/api/v1/shops/20/status")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void unsupportedStatusIsRejectedBeforeService() throws Exception {
        MockMvc mvc = mvc(org.mockito.Mockito.mock(ShopService.class));
        mvc.perform(patch("/api/v1/shops/20/status")
                        .principal(authentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static MockMvc mvc(ShopService service) {
        return MockMvcBuilders.standaloneSetup(new ShopController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters((request, response, chain) -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            (UsernamePasswordAuthenticationToken) ((HttpServletRequest) request).getUserPrincipal());
                    try { chain.doFilter(request, response); } finally { SecurityContextHolder.clearContext(); }
                }).build();
    }

    private static UsernamePasswordAuthenticationToken authentication() {
        UserPrincipal principal = new UserPrincipal(12L, "MERCHANT");
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities());
    }
}

