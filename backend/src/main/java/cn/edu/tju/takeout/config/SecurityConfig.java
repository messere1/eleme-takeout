package cn.edu.tju.takeout.config;

import cn.edu.tju.takeout.auth.JwtService;
import cn.edu.tju.takeout.auth.JwtAuthenticationFilter;
import cn.edu.tju.takeout.auth.SecurityErrorWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtService jwtService() {
        return new JwtService("replace-this-development-secret-before-production", Duration.ofHours(2));
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthenticationFilter jwtFilter, ObjectMapper objectMapper)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                SecurityErrorWriter.write(
                                        response, objectMapper, 401,
                                        "AUTH_INVALID", "请先登录"))
                        .accessDeniedHandler((request, response, exception) ->
                                SecurityErrorWriter.write(
                                        response, objectMapper, 403,
                                        "FORBIDDEN", "无权访问该资源")))
                .authorizeHttpRequests(auth -> auth
                        // 错误页放行：否则后端内部错误/404 会经 /error 被拦成 401“请先登录”
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login", "/api/v1/users", "/api/v1/merchants")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/**").permitAll()
                        .requestMatchers(
                                HttpMethod.GET, 
                                "/api/v1/categories/*/products",
                                "/api/v1/products/*"
                        ).permitAll()
                        .requestMatchers("/api/v1/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/users/me").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/*")
                        .hasAnyRole("CUSTOMER", "MERCHANT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/cancel")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/merchant/orders")
                        .hasRole("MERCHANT")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/shops/**")
                        .hasRole("MERCHANT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/shops/*/categories",
                                "/api/v1/shops/*/products")
                        .hasRole("MERCHANT")
                        .requestMatchers("/api/v1/categories/**", "/api/v1/products/**")
                        .hasRole("MERCHANT")
                        .requestMatchers("/api/v1/merchant/**")
                        .hasRole("MERCHANT")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
