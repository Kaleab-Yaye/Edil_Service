package com.edil.controller;

import com.edil.service.CreatorOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorOnboardingService onboardingService;

    @PostMapping("/reapply")
    public ResponseEntity<Void> reapply(Authentication authentication) {
        onboardingService.reapply(authentication.getName());
        return ResponseEntity.ok().build();
    }
}
