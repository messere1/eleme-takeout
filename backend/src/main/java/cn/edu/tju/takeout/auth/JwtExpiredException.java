package cn.edu.tju.takeout.auth;

public class JwtExpiredException extends IllegalArgumentException {
    public JwtExpiredException() { super("令牌已过期"); }
}
