package com.example.slack.auth.web;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.slack.auth.jwt.JwtTokenProvider;
import com.example.slack.auth.model.LoginUser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuth2(Google/Slack OIDC) 로그인 성공 시, 표준 OIDC 클레임을 {@link LoginUser}로 매핑하고
 * 자체 JWT 액세스/리프레시 토큰을 HttpOnly 쿠키로 발급한 뒤 홈으로 리다이렉트한다.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieFactory cookieFactory;

    public OAuth2LoginSuccessHandler(final JwtTokenProvider jwtTokenProvider,
                                     final AuthCookieFactory cookieFactory) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.cookieFactory = cookieFactory;
        setDefaultTargetUrl("/home");
    }

    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        LoginUser user = toLoginUser(oauthUser);

        addCookie(response, cookieFactory.accessCookie(jwtTokenProvider.createAccessToken(user)));
        addCookie(response, cookieFactory.refreshCookie(jwtTokenProvider.createRefreshToken(user)));

        super.onAuthenticationSuccess(request, response, authentication);
    }

    /** Google·Slack 모두 OIDC 표준 클레임(sub/name/email/picture)을 제공한다. */
    private LoginUser toLoginUser(final OAuth2User oauthUser) {
        return new LoginUser(
            oauthUser.getAttribute("sub"),
            oauthUser.getAttribute("name"),
            oauthUser.getAttribute("email"),
            oauthUser.getAttribute("picture"));
    }

    private void addCookie(final HttpServletResponse response, final ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
