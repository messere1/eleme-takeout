package cn.edu.tju.takeout.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final byte[] secret;
    private final Duration lifetime;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtService(String secret, Duration lifetime) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.lifetime = lifetime;
    }

    public String issue(String subject, String role) {
        try {
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", subject);
            payload.put("role", role);
            payload.put("exp", Instant.now().plus(lifetime).getEpochSecond());
            String body = encode(objectMapper.writeValueAsBytes(payload));
            String content = header + "." + body;
            return content + "." + encode(sign(content));
        } catch (Exception exception) {
            throw new IllegalStateException("无法签发令牌", exception);
        }
    }

    public JwtClaims parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !java.security.MessageDigest.isEqual(sign(parts[0] + "." + parts[1]), DECODER.decode(parts[2]))) {
                throw new IllegalArgumentException("令牌签名无效");
            }
            Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), new TypeReference<>() {});
            JwtClaims claims = new JwtClaims(
                    String.valueOf(payload.get("sub")), String.valueOf(payload.get("role")),
                    Instant.ofEpochSecond(((Number) payload.get("exp")).longValue()));
            if (!claims.expiresAt().isAfter(Instant.now())) {
                throw new JwtExpiredException();
            }
            return claims;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("令牌无效", exception);
        }
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(byte[] value) { return ENCODER.encodeToString(value); }
}
