package com.edil.controller;

import com.edil.dto.response.CampaignResponse;
import com.edil.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/campaigns")
@RequiredArgsConstructor
public class AdminCampaignController {

    private final CampaignService campaignService;

    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT_ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<CampaignResponse>> getPendingCampaigns() {
        return ResponseEntity.ok(campaignService.getPendingCampaigns());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT_ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveCampaign(@PathVariable UUID id) {
        campaignService.approveCampaign(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT_ADMIN')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectCampaign(@PathVariable UUID id) {
        campaignService.rejectCampaign(id);
        return ResponseEntity.ok().build();
    }
}
