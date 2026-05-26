package com.example.slack.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.slack.auth.jwt.JwtTokenProvider;
import com.example.slack.auth.model.LoginUser;
import com.example.slack.auth.web.AuthCookieFactory;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 토큰 관련 엔드포인트.
 *
 * <p>OAuth 인가·콜백은 Spring Security(oauth2Login)가 처리하므로,
 * 여기서는 토큰 재발급과 로그아웃만 담당한다.
 */
@Controller
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieFactory cookieFactory;

    public AuthController(final JwtTokenProvider jwtTokenProvider,
                          final AuthCookieFactory cookieFactory) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieFactory = cookieFactory;
    }

    /**
     * 리프레시 토큰으로 액세스 토큰을 재발급한다(리프레시 토큰도 함께 회전).
     */
    @GetMapping("/oauth/refresh")
    public String refresh(@CookieValue(name = AuthCookieFactory.REFRESH_TOKEN, required = false) final String refreshToken,
                          final HttpServletResponse response) {
        LoginUser user = jwtTokenProvider.parseRefreshToken(refreshToken);
        if (user == null) {
            clearTokens(response);
            return "redirect:/login";
        }
        issueTokens(user, response);
        return "redirect:/home";
    }

    /**
     * 로그아웃. 토큰 쿠키를 만료시킨다.
     */
    @GetMapping("/logout")
    public String logout(final HttpServletResponse response) {
        clearTokens(response);
        return "redirect:/login";
    }

    private void issueTokens(final LoginUser user, final HttpServletResponse response) {
        addCookie(response, cookieFactory.accessCookie(jwtTokenProvider.createAccessToken(user)));
        addCookie(response, cookieFactory.refreshCookie(jwtTokenProvider.createRefreshToken(user)));
    }

    private void clearTokens(final HttpServletResponse response) {
        addCookie(response, cookieFactory.expire(AuthCookieFactory.ACCESS_TOKEN));
        addCookie(response, cookieFactory.expire(AuthCookieFactory.REFRESH_TOKEN));
    }

    private void addCookie(final HttpServletResponse response, final ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
