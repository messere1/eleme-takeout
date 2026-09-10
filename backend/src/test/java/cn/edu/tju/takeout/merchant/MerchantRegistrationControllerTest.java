package cn.edu.tju.takeout.merchant;

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

class MerchantRegistrationControllerTest {
    @Test
    void validMerchantRegistrationReturnsCreated() throws Exception {
        MerchantService service = org.mockito.Mockito.mock(MerchantService.class);
        when(service.register(any())).thenReturn(new MerchantView(12L, "北洋餐厅", "13800138000", "中式快餐", 20L, "CLOSED"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new MerchantController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"merchantName":"北洋餐厅","phone":"13800138000","password":"abc12345","businessScope":"中式快餐","shopAddress":"天津大学北洋园校区"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.merchantName").value("北洋餐厅"))
                .andExpect(jsonPath("$.data.shopStatus").value("CLOSED"));
    }
}
