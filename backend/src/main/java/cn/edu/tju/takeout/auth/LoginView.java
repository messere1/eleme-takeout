package cn.edu.tju.takeout.auth;

public record LoginView(String token, String role, long expiresIn) {
}
