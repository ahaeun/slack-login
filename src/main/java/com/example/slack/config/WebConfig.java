package com.example.slack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.slack.auth.web.AuthInterceptor;

/**
 * 인증 인터셉터 등록. 공개 경로를 제외한 나머지는 모두 로그인 보호 대상이다(secure by default).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(final AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/", "/login",       // 로그인 페이지
                "/oauth/**",         // Slack 인가·콜백·토큰 재발급
                "/logout",           // 로그아웃
                "/css/**", "/js/**", "/images/**", "/favicon.ico", // 정적 리소스
                "/error"             // 기본 에러 페이지
            );
    }
}
