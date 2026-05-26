package com.example.slack.auth.model;

/**
 * Slack 인증으로 식별된 로그인 사용자 정보.
 */
public record LoginUser(String id, String name, String email, String avatar) {
}
