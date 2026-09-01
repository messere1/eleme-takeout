package cn.edu.tju.takeout.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class JwtServiceRobustnessTest {
    private static final String SECRET = "replace-this-development-secret-before-production";
    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(2));

    @Test
    void changedPayloadWithOriginalSignatureIsRejected() {
        String token = jwtService.issue("7", "CUSTOMER");
        String[] parts = token.split("\\.");
        String changedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"8\",\"role\":\"CUSTOMER\",\"exp\":9999999999}"
                        .getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> jwtService.parse(parts[0] + "." + changedPayload + "." + parts[2]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("令牌签名无效");
    }

    @Test
    void correctlySignedPayloadWithInvalidExpiryTypeIsRejected() throws Exception {
        String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = encode("{\"sub\":\"7\",\"role\":\"CUSTOMER\",\"exp\":\"later\"}");
        String content = header + "." + payload;
        String token = content + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(content));

        assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("令牌无效");
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }
}
