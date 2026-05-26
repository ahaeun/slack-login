package com.example.slack;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;

import com.example.slack.auth.model.LoginUser;
import com.example.slack.auth.web.AuthInterceptor;

/**
 * 로그인 후 진입하는 홈 화면.
 *
 * <p>토큰 검증은 {@link AuthInterceptor}가 담당하므로, 여기서는 검증된 사용자만 받는다.
 */
@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(@RequestAttribute(AuthInterceptor.LOGIN_USER) final LoginUser user,
                       final Model model) {
        model.addAttribute("user", user);
        return "home";
    }
}
