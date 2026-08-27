package com.edil.controller;

import com.edil.dto.response.UserMeResponse;
import com.edil.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserMeResponse getMe(Authentication authentication) {
        String email = authentication.getName();
        return userService.getMe(email);
    }
}
