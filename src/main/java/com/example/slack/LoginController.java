package com.example.slack;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 로그인 화면을 제공하는 컨트롤러.
 *
 * <p>현재는 화면(퍼블리싱)만 담당하며, Slack OAuth 인증 연동은 추후 추가한다.
 */
@Controller
public class LoginController {

    @GetMapping({"/", "/login"})
    public String login() {
        return "login";
    }
}
