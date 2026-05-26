package com.example.slack;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.slack.auth.model.LoginUser;

/**
 * 로그인 후 진입하는 홈 화면.
 *
 * <p>인증은 SecurityFilterChain + JwtAuthenticationFilter가 보장하므로,
 * 여기서는 인증된 사용자(principal)만 받는다.
 */
@Controller
public class HomeController {

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal final LoginUser user, final Model model) {
        model.addAttribute("user", user);
        return "home";
    }
}
