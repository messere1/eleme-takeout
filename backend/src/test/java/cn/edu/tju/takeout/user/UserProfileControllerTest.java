package cn.edu.tju.takeout.user;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

class UserProfileControllerTest {
    @Test
    void currentUserCanReadOnlyTheirOwnProfile() throws Exception {
        UserService service = org.mockito.Mockito.mock(UserService.class);
        when(service.getProfile(7L)).thenReturn(new UserView(7L, "alice", "13800138000", "Alice", "天津"));
        MockMvc mvc = mvc(service);

        mvc.perform(get("/api/v1/users/me").principal(authentication(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.nickname").value("Alice"));
        verify(service).getProfile(7L);
    }

    @Test
    void currentUserCanUpdateTheirOwnProfile() throws Exception {
        UserService service = org.mockito.Mockito.mock(UserService.class);
        when(service.updateProfile(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new UserView(7L, "alice", "13900139000", "Alice同学", "北洋园"));
        MockMvc mvc = mvc(service);

        mvc.perform(patch("/api/v1/users/me")
                        .principal(authentication(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"Alice同学","phone":"13900139000","address":"北洋园"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("13900139000"));
    }

    private static MockMvc mvc(UserService service) {
        return MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters((request, response, chain) -> {
                    SecurityContextHolder.getContext().setAuthentication(
                            (UsernamePasswordAuthenticationToken)
                                    ((HttpServletRequest) request).getUserPrincipal());
                    try {
                        chain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                })
                .build();
    }

    private static UsernamePasswordAuthenticationToken authentication(long id) {
        UserPrincipal principal = new UserPrincipal(id, "CUSTOMER");
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities());
    }
}
