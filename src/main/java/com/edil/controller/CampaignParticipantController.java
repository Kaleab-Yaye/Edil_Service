package com.edil.controller;


import com.edil.dto.request.AddParticipantToCampaignRequest;
import com.edil.dto.request.CanParticipantJoinCampaignRequest;
import com.edil.dto.request.CreateCampaignSlotForUserRequest;
import com.edil.dto.request.FetchOnGoingSlotInformationForUserResponse;
import com.edil.dto.response.AddParticipantToCampaignResponse;
import com.edil.dto.response.CanParticipantJoinCampaignResponse;
import com.edil.dto.response.CreateCampaignSlotForUserResponse;
import com.edil.service.CampaignParticipantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/participant")
public class CampaignParticipantController {
    private final CampaignParticipantService campaignParticipantService;

    CampaignParticipantController(CampaignParticipantService campaignParticipantService) {
        this.campaignParticipantService = campaignParticipantService;

    }
    // /apit/v1/participant/add/participant

    @PostMapping("/can/join")
    public ResponseEntity<CanParticipantJoinCampaignResponse> canUserJoinCampaign(@RequestBody CanParticipantJoinCampaignRequest canParticipantJoinCampaignRequest, @AuthenticationPrincipal String userEmail) {
        log.info(" the user prinsciaple extracted has the value {}", userEmail);
        return campaignParticipantService.canParticipantJoinCampaign(canParticipantJoinCampaignRequest, userEmail);

    }

    @GetMapping("/ongoing/payment")

    public ResponseEntity<FetchOnGoingSlotInformationForUserResponse> handleOnGoingSlotReservation(@AuthenticationPrincipal String userEmail) {
        log.info("passing the user email of {}", userEmail);
        return campaignParticipantService.fetchOngoingUserSlotInfo(userEmail);
    }

    @PostMapping("/reserve/slot")
    public ResponseEntity<CreateCampaignSlotForUserResponse> creatCampaignSlotHandler(@RequestBody CreateCampaignSlotForUserRequest createCampaignSlotForUserRequest, @AuthenticationPrincipal String userEmail) {
        return campaignParticipantService.createCampaignSlotForUser(createCampaignSlotForUserRequest, userEmail);
    }

    @PostMapping("/add/participant")
    public ResponseEntity<AddParticipantToCampaignResponse> addParticipantHandler(@RequestBody AddParticipantToCampaignRequest addParticipantToCampaignRequest, @AuthenticationPrincipal String userEmail) {
        log.info("passing user email of {}", userEmail);
        return campaignParticipantService.AddCampaignParticipant(addParticipantToCampaignRequest, userEmail);
    }
}






