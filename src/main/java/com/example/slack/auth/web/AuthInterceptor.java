package com.example.slack.auth.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.slack.auth.jwt.JwtTokenProvider;
import com.example.slack.auth.model.LoginUser;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 보호 경로 진입 전 액세스 토큰을 검증하는 인터셉터.
 *
 * <ul>
 *   <li>유효하면 {@link LoginUser}를 요청 속성({@value #LOGIN_USER})에 담고 통과시킨다.</li>
 *   <li>만료·부재 시 리프레시 토큰이 있으면 재발급 경로로, 없으면 로그인 페이지로 보낸다.</li>
 * </ul>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 컨트롤러에서 {@code @RequestAttribute}로 꺼내 쓰는 키. */
    public static final String LOGIN_USER = "loginUser";

    private final JwtTokenProvider jwtTokenProvider;

    public AuthInterceptor(final JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {

        String accessToken = readCookie(request, AuthCookieFactory.ACCESS_TOKEN);
        LoginUser user = jwtTokenProvider.parseAccessToken(accessToken);

        if (user != null) {
            request.setAttribute(LOGIN_USER, user);
            return true;
        }

        // 액세스 토큰이 없거나 만료됨 → 리프레시 토큰이 있으면 재발급, 없으면 로그인으로
        String refreshToken = readCookie(request, AuthCookieFactory.REFRESH_TOKEN);
        String target = (refreshToken != null && !refreshToken.isBlank())
            ? "/oauth/refresh"
            : "/login";

        response.sendRedirect(request.getContextPath() + target);
        return false;
    }

    private String readCookie(final HttpServletRequest request, final String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
