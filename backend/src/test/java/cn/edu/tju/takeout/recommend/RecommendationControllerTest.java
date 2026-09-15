package cn.edu.tju.takeout.recommend;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.GlobalExceptionHandler;
import cn.edu.tju.takeout.shop.ShopView;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecommendationControllerTest {
    private final RecommendationService service = mock(RecommendationService.class);
    private final MockMvc mvc = mvc(service);

    /** 游客没有个人历史，必须退回全站热度而不是报错 */
    @Test
    void guestIsServedPopularityRecommendations() throws Exception {
        when(service.recommendShops(null, 6))
                .thenReturn(List.of(new ShopView(1L, 12L, "北洋餐厅", "欢迎光临", "OPEN")));

        mvc.perform(get("/api/v1/recommendations/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].shopName").value("北洋餐厅"));

        verify(service).recommendShops(null, 6);
    }

    /** 登录顾客必须按自己的 userId 取历史，不能退化成全站热度 */
    @Test
    void loggedInCustomerIsRecommendedAgainstOwnUserId() throws Exception {
        when(service.recommendShops(7L, 3)).thenReturn(List.of());

        mvc.perform(get("/api/v1/recommendations/shops")
                        .param("limit", "3")
                        .principal(authentication(new UserPrincipal(7L, "CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(service).recommendShops(7L, 3);
    }

    /** 商家、管理员和骑手不能借公开推荐接口读取同编号顾客的个人偏好。 */
    @Test
    void nonCustomerPrincipalIsServedOnlyPopularityRecommendations() throws Exception {
        when(service.recommendShops(null, 6)).thenReturn(List.of());

        mvc.perform(get("/api/v1/recommendations/shops")
                        .principal(authentication(new UserPrincipal(7L, "MERCHANT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(service).recommendShops(null, 6);
    }

    @Test
    void nonNumericLimitReturnsTraceableBadRequestWithoutCallingService() throws Exception {
        mvc.perform(get("/api/v1/recommendations/shops").param("limit", "six"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        verifyNoInteractions(service);
    }

    private static MockMvc mvc(RecommendationService service) {
        return MockMvcBuilders.standaloneSetup(new RecommendationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters((request, response, chain) -> {
                    Principal user = ((HttpServletRequest) request).getUserPrincipal();
                    if (user instanceof UsernamePasswordAuthenticationToken token) {
                        SecurityContextHolder.getContext().setAuthentication(token);
                    }
                    try {
                        chain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                })
                .build();
    }

    private static UsernamePasswordAuthenticationToken authentication(UserPrincipal principal) {
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.authorities());
    }
}
