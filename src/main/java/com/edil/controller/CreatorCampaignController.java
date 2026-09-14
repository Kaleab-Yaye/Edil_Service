package com.edil.controller;

import com.edil.domain.Account;
import com.edil.dto.request.CreateCampaignRequest;
import com.edil.dto.request.EndCampaignByCreatorRequest;
import com.edil.dto.response.CampaignResponse;
import com.edil.dto.response.EndCampaignByCreatorResponse;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.service.CampaignService;
import com.edil.service.CreatorsCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/creator/campaigns")
@RequiredArgsConstructor
public class CreatorCampaignController {

    private final CampaignService campaignService;
    private final CreatorsCampaignService creatorsCampaignService;
    private final AccountRepository accountRepository;

    @PreAuthorize("hasRole('CREATOR')")
    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request, Authentication authentication) {
        CampaignResponse createdCampaign = campaignService.createCampaign(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCampaign);
    }

    @PreAuthorize("hasRole('CREATOR')")
    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getCreatorCampaigns(Authentication authentication) {
        Account account = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        return ResponseEntity.ok(campaignService.getCampaignsByCreatorId(account.getId()));
    }

    @PreAuthorize("hasRole('CREATOR')")
    @PostMapping("/end/campaign")

    public ResponseEntity<EndCampaignByCreatorResponse> endCampaignController(@RequestBody EndCampaignByCreatorRequest endCampaignByCreatorRequest, @AuthenticationPrincipal String email){
        return  creatorsCampaignService.endCampaignByCreator(endCampaignByCreatorRequest, email);
    }


}
