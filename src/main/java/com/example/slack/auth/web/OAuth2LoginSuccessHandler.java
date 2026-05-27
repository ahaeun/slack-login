package com.example.slack.auth.web;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.slack.auth.jwt.JwtTokenProvider;
import com.example.slack.auth.model.LoginUser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuth2 로그인 성공 시, 공급자별 사용자 정보를 {@link LoginUser}로 매핑하고
 * 자체 JWT 액세스/리프레시 토큰을 HttpOnly 쿠키로 발급한 뒤 홈으로 리다이렉트한다.
 *
 * <ul>
 *   <li>Google·Slack: OIDC 표준 클레임(sub/name/email/picture)</li>
 *   <li>Naver: OAuth2(비 OIDC)라 사용자 정보가 {@code response} 객체 안에 중첩됨</li>
 * </ul>
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

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();
        LoginUser user = toLoginUser(registrationId, token.getPrincipal());

        addCookie(response, cookieFactory.accessCookie(jwtTokenProvider.createAccessToken(user)));
        addCookie(response, cookieFactory.refreshCookie(jwtTokenProvider.createRefreshToken(user)));

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private LoginUser toLoginUser(final String registrationId, final OAuth2User oauthUser) {
        if ("naver".equals(registrationId)) {
            // 네이버는 { "response": { id, name, email, profile_image } } 구조
            Map<String, Object> r = oauthUser.getAttribute("response");
            if (r == null) {
                r = Map.of();
            }
            return new LoginUser(
                str(r.get("id")),
                str(r.get("name")),
                str(r.get("email")),
                str(r.get("profile_image")));
        }

        // Google·Slack: OIDC 표준 클레임
        return new LoginUser(
            oauthUser.getAttribute("sub"),
            oauthUser.getAttribute("name"),
            oauthUser.getAttribute("email"),
            oauthUser.getAttribute("picture"));
    }

    private String str(final Object value) {
        return value == null ? null : value.toString();
    }

    private void addCookie(final HttpServletResponse response, final ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
