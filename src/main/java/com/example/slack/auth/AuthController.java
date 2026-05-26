package com.example.slack.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.slack.auth.jwt.JwtTokenProvider;
import com.example.slack.auth.model.LoginUser;
import com.example.slack.auth.web.AuthCookieFactory;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

    /** "Sign in with Slack" 플로우에 필요한 사용자 스코프. */
    private static final String USER_SCOPE = "identity.basic,identity.email,identity.avatar";

    private final String clientId;
    private final String redirectUrl;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieFactory cookieFactory;

    public AuthController(@Value("${slack.client-id}") final String clientId,
                          @Value("${slack.redirect-url}") final String redirectUrl,
                          final AuthService authService,
                          final JwtTokenProvider jwtTokenProvider,
                          final AuthCookieFactory cookieFactory) {
        this.clientId = clientId;
        this.redirectUrl = redirectUrl;
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieFactory = cookieFactory;
    }

    /**
     * 로그인 버튼 클릭 시 진입점. Slack 인가 페이지로 리다이렉트한다.
     */
    @GetMapping("/oauth/slack")
    public String authorize() {
        String authorizeUrl = UriComponentsBuilder
            .fromUriString("https://slack.com/oauth/v2/authorize")
            .queryParam("client_id", clientId)
            .queryParam("user_scope", USER_SCOPE)
            .queryParam("redirect_uri", redirectUrl)
            .encode()
            .toUriString();

        return "redirect:" + authorizeUrl;
    }

    /**
     * Slack 콜백. 인가 코드로 로그인 후, 자체 JWT 액세스/리프레시 토큰을 쿠키로 발급한다.
     */
    @GetMapping("/oauth/slack/callback")
    public String callback(@RequestParam("code") final String code,
                           final HttpServletResponse response) {
        LoginUser user = authService.login(code);
        issueTokens(user, response);
        return "redirect:/home";
    }

    /**
     * 리프레시 토큰으로 액세스 토큰을 재발급한다(리프레시 토큰도 함께 회전).
     */
    @GetMapping("/oauth/refresh")
    public String refresh(@CookieValue(name = AuthCookieFactory.REFRESH_TOKEN, required = false) final String refreshToken,
                          final HttpServletResponse response) {
        LoginUser user = jwtTokenProvider.parseRefreshToken(refreshToken);
        if (user == null) {
            // 리프레시 토큰이 없거나 만료 → 재로그인
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
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        addCookie(response, cookieFactory.accessCookie(accessToken));
        addCookie(response, cookieFactory.refreshCookie(refreshToken));
    }

    private void clearTokens(final HttpServletResponse response) {
        addCookie(response, cookieFactory.expire(AuthCookieFactory.ACCESS_TOKEN));
        addCookie(response, cookieFactory.expire(AuthCookieFactory.REFRESH_TOKEN));
    }

    private void addCookie(final HttpServletResponse response, final ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
