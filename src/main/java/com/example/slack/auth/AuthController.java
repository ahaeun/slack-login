package com.example.slack.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class AuthController {

    /** "Sign in with Slack" 플로우에 필요한 사용자 스코프. */
    private static final String USER_SCOPE = "identity.basic,identity.email,identity.avatar";

    private final String clientId;
    private final String redirectUrl;
    private final AuthService authService;

    public AuthController(@Value("${slack.client-id}") final String clientId,
                          @Value("${slack.redirect-url}") final String redirectUrl,
                          final AuthService authService) {
        this.clientId = clientId;
        this.redirectUrl = redirectUrl;
        this.authService = authService;
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
     * Slack이 인가 후 돌려주는 콜백. 전달받은 code로 토큰 교환·로그인을 수행한다.
     */
    @GetMapping("/oauth/slack/callback")
    public String callback(@RequestParam("code") final String code) {
        authService.login(code);
        return "redirect:/";
    }
}
