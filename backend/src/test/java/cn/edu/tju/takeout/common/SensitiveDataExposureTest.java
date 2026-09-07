package cn.edu.tju.takeout.common;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.takeout.auth.LoginRequest;
import cn.edu.tju.takeout.merchant.MerchantRegistrationRequest;
import cn.edu.tju.takeout.user.UserRegistrationRequest;
import org.junit.jupiter.api.Test;

class SensitiveDataExposureTest {

    @Test
    void authenticationRequestsDoNotExposePasswordsInDiagnosticText() {
        String secret = "Secret123";

        assertThat(new LoginRequest("13800138000", secret, "CUSTOMER").toString())
                .doesNotContain(secret);
        assertThat(new UserRegistrationRequest("user001", "13800138000", secret).toString())
                .doesNotContain(secret);
        assertThat(new MerchantRegistrationRequest(
                "北洋餐厅", "13900139000", secret, "中式快餐").toString())
                .doesNotContain(secret);
    }

    @Test
    void authenticationRequestsMaskCompletePhoneNumbersInDiagnosticText() {
        String phone = "13800138000";

        assertThat(new LoginRequest(phone, "Secret123", "CUSTOMER").toString())
                .doesNotContain(phone);
        assertThat(new UserRegistrationRequest("user001", phone, "Secret123").toString())
                .doesNotContain(phone);
    }
}
