package cn.edu.tju.takeout.user;

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

class UserRegistrationControllerTest {

    @Test
    void validRegistrationReturnsCreatedEnvelope() throws Exception {
        UserService service = org.mockito.Mockito.mock(UserService.class);
        when(service.register(any())).thenReturn(new UserView(1L, "alice", "13800138000", null, null));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","phone":"13800138000","password":"abc12345"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void invalidRegistrationIsRejectedBeforeService() throws Exception {
        UserService service = org.mockito.Mockito.mock(UserService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"a","phone":"123","password":"plain"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.fieldErrors.username").exists())
                .andExpect(jsonPath("$.data.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.data.fieldErrors.password").exists());
    }
}

