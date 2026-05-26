package com.example.slack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

import com.example.slack.auth.web.AuthCookieFactory;
import com.example.slack.auth.web.JwtAuthenticationFilter;
import com.example.slack.auth.web.OAuth2LoginSuccessHandler;

import jakarta.servlet.http.Cookie;

/**
 * Spring Security 설정.
 *
 * <ul>
 *   <li>로그인 플로우(Slack/Google OIDC)는 {@code oauth2Login}이 담당</li>
 *   <li>로그인 성공 시 {@link OAuth2LoginSuccessHandler}가 자체 JWT 쿠키를 발급</li>
 *   <li>이후 요청 인증은 {@link JwtAuthenticationFilter}가 쿠키의 JWT로 처리(무상태)</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/", "/login", "/oauth/refresh", "/logout",
        "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(final JwtAuthenticationFilter jwtAuthenticationFilter,
                          final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
            // JWT 쿠키 기반이라 CSRF 토큰 대신 SameSite 쿠키로 대응(추후 보강 가능)
            .csrf(AbstractHttpConfigurer::disable)
            // 로그아웃은 자체 컨트롤러(/logout)에서 쿠키 만료로 처리
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated())
            .oauth2Login(oauth -> oauth
                .successHandler(oAuth2LoginSuccessHandler))
            // 인증 결과를 세션에 저장하지 않음 → 앱 요청은 JWT 쿠키로만 인증(무상태)
            .securityContext(ctx -> ctx.securityContextRepository(new NullSecurityContextRepository()))
            // 미인증 보호 경로 접근 시: 리프레시 토큰 있으면 재발급, 없으면 로그인 페이지
            .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            String target = hasRefreshCookie(request) ? "/oauth/refresh" : "/login";
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    private boolean hasRefreshCookie(final jakarta.servlet.http.HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (Cookie cookie : request.getCookies()) {
            if (AuthCookieFactory.REFRESH_TOKEN.equals(cookie.getName())
                    && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }
}
