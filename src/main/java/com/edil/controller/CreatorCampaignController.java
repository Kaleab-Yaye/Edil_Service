package com.edil.controller;

import com.edil.domain.Account;
import com.edil.dto.request.*;
import com.edil.dto.response.*;
import com.edil.exception.AccountNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.service.CampaignService;
import com.edil.service.CreatorsCampaignService;
import com.edil.util.ArchiveCampaignAndGeneratePdfUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/creator/campaigns")
@RequiredArgsConstructor
public class CreatorCampaignController {

    private final CampaignService campaignService;
    private final CreatorsCampaignService creatorsCampaignService;
    private final AccountRepository accountRepository;
    private final ArchiveCampaignAndGeneratePdfUtil archiveCampaignAndGeneratePdfUtil;

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


    @PreAuthorize("hasRole('CREATOR')")
    @GetMapping("/report")

    public ResponseEntity<GetPdfInfoResponse> getPdfInfoHandler(@RequestParam("id")UUID uuid,  @AuthenticationPrincipal String email){
        return  creatorsCampaignService.getPdfForCreator(new GetPdfInfoRequest(uuid), email);
    }

    @PreAuthorize("hasRole('CREATOR')")
    @GetMapping("/report/download/key")

    public ResponseEntity<GetDownloadPdfKeyResponse> getDownloadPdfKeyHandler(@RequestParam("id") UUID campaignId,@AuthenticationPrincipal String email ){
        return creatorsCampaignService.getPdfDownloadKey(new GetDownloadPdfKeyRequest(campaignId), email);
    }

    @PreAuthorize("hasRole('CREATOR')")
    @PostMapping("/joined/user")

    public ResponseEntity<GetJoinedPlayerInfoWithEdilNumberResponse> getJoinedInfoWithEdilNumberHandler(@Validated @RequestBody GetJoinedPlayerInfoWithEdilNumberRequest getJoinedRequest, @AuthenticationPrincipal String email ){

        log.info("intered the getJoinedUser with edil code contoroller with email {} and the campaign id of {}", email, getJoinedRequest.campaignId());

        return  creatorsCampaignService.getJoinedPlayerWithEdilNumber(getJoinedRequest, email);
    }



}
