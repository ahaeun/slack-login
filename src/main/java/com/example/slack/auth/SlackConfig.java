package com.example.slack.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;

/**
 * Slack SDK 관련 빈 설정.
 */
@Configuration
public class SlackConfig {

    /**
     * Slack Web API 호출용 {@link MethodsClient}.
     *
     * <p>OAuth 토큰 교환·사용자 조회 등은 호출 시점에 토큰을 전달하므로,
     * 토큰 없이 생성한 단일 클라이언트를 공유한다.
     */
    @Bean
    public MethodsClient slackMethodsClient() {
        return Slack.getInstance().methods();
    }
}
