package com.example.slack.auth.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 로그인 토큰을 담는 HttpOnly 쿠키를 생성한다.
 */
@Component
public class AuthCookieFactory {

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";

    private final boolean secure;
    private final long accessValiditySeconds;
    private final long refreshValiditySeconds;

    public AuthCookieFactory(
            @Value("${jwt.cookie-secure:false}") final boolean secure,
            @Value("${jwt.access-token-validity}") final long accessValidityMillis,
            @Value("${jwt.refresh-token-validity}") final long refreshValidityMillis) {
        this.secure = secure;
        this.accessValiditySeconds = accessValidityMillis / 1000;
        this.refreshValiditySeconds = refreshValidityMillis / 1000;
    }

    public ResponseCookie accessCookie(final String token) {
        return build(ACCESS_TOKEN, token, accessValiditySeconds);
    }

    public ResponseCookie refreshCookie(final String token) {
        return build(REFRESH_TOKEN, token, refreshValiditySeconds);
    }

    /** 로그아웃 시 즉시 만료시키는 빈 쿠키. */
    public ResponseCookie expire(final String name) {
        return build(name, "", 0);
    }

    private ResponseCookie build(final String name, final String value, final long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite("Lax")
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
    }
}
