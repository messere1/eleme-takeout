package cn.edu.tju.takeout.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.edu.tju.takeout.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserLoginControllerTest {
    @Test
    void validCredentialsReturnTokenRoleAndExpiry() throws Exception {
        AuthService service = org.mockito.Mockito.mock(AuthService.class);
        when(service.login(any())).thenReturn(new LoginView("token", "CUSTOMER", 7200));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"alice","password":"abc12345","role":"CUSTOMER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("token"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200));
    }
}

