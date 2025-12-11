package com.ece.dental_clinic.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthRedirectController {

    @GetMapping("/after-login")
    public String afterLogin(Authentication authentication) {
        boolean isDentist = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DENTIST"));

        if (isDentist) {
            return "redirect:/dentist/home";
        } else {
            return "redirect:/patient/home";
        }
    }
}