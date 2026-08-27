package com.edil.controller;

import com.edil.dto.request.CreateCampaignRequest;
import com.edil.dto.response.CampaignResponse;
import com.edil.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/creator/campaigns")
@RequiredArgsConstructor
public class CreatorCampaignController {

    private final CampaignService campaignService;

    @PreAuthorize("hasRole('CREATOR')")
    @PostMapping
    public ResponseEntity<Void> createCampaign(@Valid @RequestBody CreateCampaignRequest request, Authentication authentication) {
        // Assuming email is the principal name. Adjust based on UserDetails implementation.
        campaignService.createCampaign(authentication.getName(), request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('CREATOR')")
    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getCreatorCampaigns(Authentication authentication) {
        // Assuming the ID is available in the authentication. If it's email, we'd need to fetch Account first.
        // For simplicity, let's assume authentication.getPrincipal() can be mapped to UUID or we fetch it.
        // Update this line according to your Security context setup
        UUID accountId = UUID.fromString(authentication.getName()); 
        return ResponseEntity.ok(campaignService.getCampaignsByCreatorId(accountId));
    }
}
