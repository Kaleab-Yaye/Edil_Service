package com.edil.controller;

import com.edil.dto.response.CreatorQueueItemResponse;
import com.edil.service.CreatorOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/creators")
@RequiredArgsConstructor
public class AdminCreatorController {

    private final CreatorOnboardingService onboardingService;

    @GetMapping("/queue")
    public ResponseEntity<List<CreatorQueueItemResponse>> getQueue() {
        return ResponseEntity.ok(onboardingService.getQueue());
    }

    @PostMapping("/{accountId}/approve")
    public ResponseEntity<Void> approve(@PathVariable UUID accountId) {
        onboardingService.approveCreator(accountId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{accountId}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID accountId) {
        onboardingService.rejectCreator(accountId);
        return ResponseEntity.ok().build();
    }
}
