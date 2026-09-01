package cn.edu.tju.takeout.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            JwtClaims claims = jwtService.parse(authorization.substring(7));
            Long userId = Long.valueOf(claims.subject());
            UserPrincipal principal = new UserPrincipal(userId, claims.role());
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, principal.authorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (JwtExpiredException exception) {
            SecurityContextHolder.clearContext();
            SecurityErrorWriter.write(
                    response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_EXPIRED", "令牌已过期");
        } catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            SecurityErrorWriter.write(
                    response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
                    "AUTH_INVALID", "令牌无效");
        }
    }
}
