package com.example.slack.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.slack.auth.model.LoginUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 서비스 자체 JWT 액세스/리프레시 토큰을 발급·검증한다.
 *
 * <p>토큰 타입은 {@code type} 클레임으로 구분한다(access / refresh).
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_AVATAR = "avatar";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessValidityMillis;
    private final long refreshValidityMillis;

    public JwtTokenProvider(
            @Value("${jwt.secret}") final String secret,
            @Value("${jwt.access-token-validity}") final long accessValidityMillis,
            @Value("${jwt.refresh-token-validity}") final long refreshValidityMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessValidityMillis = accessValidityMillis;
        this.refreshValidityMillis = refreshValidityMillis;
    }

    /** 사용자 정보를 담은 액세스 토큰을 발급한다. */
    public String createAccessToken(final LoginUser user) {
        return buildToken(user, TYPE_ACCESS, accessValidityMillis);
    }

    /** 재발급용 리프레시 토큰을 발급한다(식별자만 포함). */
    public String createRefreshToken(final LoginUser user) {
        return buildToken(user, TYPE_REFRESH, refreshValidityMillis);
    }

    private String buildToken(final LoginUser user, final String type, final long validityMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
            .subject(user.id())
            .claim(CLAIM_TYPE, type)
            .claim(CLAIM_NAME, user.name())
            .claim(CLAIM_EMAIL, user.email())
            .claim(CLAIM_AVATAR, user.avatar())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact();
    }

    /** 액세스 토큰을 검증하고 사용자 정보를 복원한다. 유효하지 않으면 {@code null}. */
    public LoginUser parseAccessToken(final String token) {
        return toUser(parse(token, TYPE_ACCESS));
    }

    /** 리프레시 토큰을 검증하고 사용자 정보를 복원한다. 유효하지 않으면 {@code null}. */
    public LoginUser parseRefreshToken(final String token) {
        return toUser(parse(token, TYPE_REFRESH));
    }

    private LoginUser toUser(final Claims claims) {
        if (claims == null) {
            return null;
        }
        return new LoginUser(
            claims.getSubject(),
            claims.get(CLAIM_NAME, String.class),
            claims.get(CLAIM_EMAIL, String.class),
            claims.get(CLAIM_AVATAR, String.class));
    }

    private Claims parse(final String token, final String expectedType) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            // 만료·서명오류·형식오류 등은 모두 "비유효 토큰"으로 처리
            return null;
        }
    }
}
