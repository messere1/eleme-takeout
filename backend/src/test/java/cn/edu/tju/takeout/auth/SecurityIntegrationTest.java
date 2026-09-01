package cn.edu.tju.takeout.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @Test
    void protectedEndpointWithoutTokenReturnsUnifiedUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID"));
    }

    @Test
    void forgedTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID"));
    }

    @Test
    void expiredTokenReturnsUnauthorized() throws Exception {
        String expired = new JwtService("replace-this-development-secret-before-production",
                Duration.ofSeconds(-1)).issue("7", "CUSTOMER");

        mockMvc.perform(get("/api/v1/cart").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_EXPIRED"));
    }

    @Test
    void merchantTokenCannotUseCustomerCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(12L, "MERCHANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void customerTokenCannotUseMerchantShopWriteEndpoint() throws Exception {
        mockMvc.perform(patch("/api/v1/shops/20/status")
                        .header("Authorization", bearer(7L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void validCustomerTokenPopulatesAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(7L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String bearer(Long userId, String role) {
        return "Bearer " + jwtService.issue(String.valueOf(userId), role);
    }
}
