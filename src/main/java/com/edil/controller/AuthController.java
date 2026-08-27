package com.edil.controller;

import com.edil.dto.request.LoginRequest;
import com.edil.dto.request.RegisterCreatorRequest;
import com.edil.dto.request.RegisterUserRequest;
import com.edil.dto.response.AuthResponse;
import com.edil.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/user")
    public AuthResponse registerUser(@Valid @RequestBody RegisterUserRequest request) {
        return authService.registerUser(request);
    }

    @PostMapping("/register/creator")
    public AuthResponse registerCreator(@Valid @RequestBody RegisterCreatorRequest request) {
        return authService.registerCreator(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
