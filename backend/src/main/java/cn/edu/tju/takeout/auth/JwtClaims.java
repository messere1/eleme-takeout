package cn.edu.tju.takeout.auth;

import java.time.Instant;

public record JwtClaims(String subject, String role, Instant expiresAt) {
}
