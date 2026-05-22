package com.example.slack.auth;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public void login(final String code) {
        try {
            // 1. token 발급 요청
            OAuthV2AccessRequest accessRequest = OAuthV2AccessRequest.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(redirectUrl)
                .code(code)
                .build();

            OAuthV2AccessResponse accessResponse = slackClient.oauthV2Access(accessRequest);
            String token = accessResponse.getAuthedUser().getAccessToken();

            // 2. Slack에 사용자 정보 조회 요청
            UsersIdentityRequest identityRequest = UsersIdentityRequest.builder()
                .token(token)
                .build();

            UsersIdentityResponse.User user = slackClient.usersIdentity(identityRequest)
                .getUser();

            // 시스템 자체 로그인 로직 수행 (ex: JWT 토큰 또는 세션ID 발급 등)

        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }

}
