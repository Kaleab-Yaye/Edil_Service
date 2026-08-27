package com.edil.controller;

import com.edil.domain.Account;
import com.edil.domain.enums.OnboardingStatus;
import com.edil.dto.response.PresignResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;
    private final AccountRepository accountRepository;

    @PreAuthorize("hasRole('CREATOR')")
    @PostMapping("/presign")
    public ResponseEntity<PresignResponse> getPresignedTicket(@RequestParam(required = false, defaultValue = ".jpg") String extension, Authentication authentication) {
        Account account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (account.getCreatorProfile() == null || account.getCreatorProfile().getOnboardingStatus() != OnboardingStatus.ONBOARDED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PresignResponse response = uploadService.generatePresignedTicket(account.getId(), extension);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth-check")
    public ResponseEntity<Void> authCheck(@RequestParam String fileId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("auth was hit and the Authentication happens to be null");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Account account = accountRepository.findByEmail(authentication.getName()).orElse(null);
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isValid = uploadService.validateTicketForNginxAuth(fileId, account.getId());
        if (isValid) {

            return ResponseEntity.ok().build();
        } else {
            log.info("the teket is invalid spring can't verfy it");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmUpload(@RequestParam String fileId, Authentication authentication) {
        Account account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        uploadService.confirmUpload(fileId, account.getId());
        return ResponseEntity.ok().build();
    }
}
