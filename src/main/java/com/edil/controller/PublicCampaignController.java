package com.edil.controller;

import com.edil.dto.response.CampaignDetailResponse;
import com.edil.dto.response.CampaignResponse;
import com.edil.dto.response.GetCampaignPaymentInfoResponse;
import com.edil.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class PublicCampaignController {

    private final CampaignService campaignService;

    @GetMapping("/ongoing")
    public ResponseEntity<Page<CampaignResponse>> getPublicCampaigns(Pageable pageable) {
        return ResponseEntity.ok(campaignService.getPublicCampaigns(pageable));
    }

    @GetMapping("/ended")
        public ResponseEntity<Page<CampaignResponse>> getEndedPublicCampaigns(Pageable pageable) {
            return ResponseEntity.ok(campaignService.getPublicCampaignsEnded(pageable));
        }



    @GetMapping("/{id}")
    public ResponseEntity<CampaignDetailResponse> getCampaignDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.getCampaignDetail(id));
    }

    @GetMapping("/payment/detail")
    public ResponseEntity<GetCampaignPaymentInfoResponse> getCampaignPaymentDetailController(@RequestParam("campaignId") UUID campaignId){
        return campaignService.getCampaignPaymentInfo(campaignId);
    }
}
