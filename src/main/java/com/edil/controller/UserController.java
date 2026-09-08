package com.edil.controller;

import com.edil.dto.request.GetUserProfileRequest;
import com.edil.dto.response.UserMeResponse;
import com.edil.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserMeResponse> handelGetUserProfile(@RequestBody GetUserProfileRequest getUserProfileRequest){
        return  userService.getUserDetails(getUserProfileRequest);
    }
}
