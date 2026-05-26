package com.example.slack.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.slack.auth.model.LoginUser;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.request.users.UsersIdentityRequest;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slack.api.methods.response.users.UsersIdentityResponse;

@Service
public class AuthService {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUrl;

    private final MethodsClient slackClient;

    public AuthService(@Value("${slack.client-id}") final String clientId,
                       @Value("${slack.client-secret}") final String clientSecret,
                       @Value("${slack.redirect-url}") final String redirectUrl,
                       final MethodsClient slackClient) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
        this.slackClient = slackClient;
    }

    /**
     * 인가 코드를 Slack 토큰으로 교환하고, 사용자 정보를 조회해 반환한다.
     *
     * @param code Slack 콜백으로 받은 인가 코드
     * @return 로그인 사용자 정보
     * @throws IllegalStateException Slack 통신 실패 시
     */
    public LoginUser login(final String code) {
        try {
            // 1. 인가 코드 → 사용자 액세스 토큰 교환
            OAuthV2AccessRequest accessRequest = OAuthV2AccessRequest.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUrl)
                .code(code)
                .build();

            OAuthV2AccessResponse accessResponse = slackClient.oauthV2Access(accessRequest);
            String token = accessResponse.getAuthedUser().getAccessToken();

            // 2. 사용자 정보 조회
            UsersIdentityRequest identityRequest = UsersIdentityRequest.builder()
                .token(token)
                .build();

            UsersIdentityResponse.User user = slackClient.usersIdentity(identityRequest).getUser();

            return new LoginUser(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getImage512());

        } catch (IOException | SlackApiException e) {
            throw new IllegalStateException("Slack 로그인 처리에 실패했습니다.", e);
        }
    }
}
