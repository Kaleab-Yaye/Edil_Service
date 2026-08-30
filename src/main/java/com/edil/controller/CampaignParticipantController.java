package com.edil.controller;


import com.edil.dto.request.AddParticipantToCampaign;
import com.edil.dto.response.AddParticipantToCampaignResponse;
import com.edil.service.CampaignParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/participant")
public class CampaignParticipantController {
    private  final CampaignParticipantService campaignParticipantService;

    CampaignParticipantController(CampaignParticipantService campaignParticipantService){
        this.campaignParticipantService = campaignParticipantService;

    }
    // /apit/v1/participant/add/participant


    @PostMapping("/add/participant")
    public ResponseEntity<AddParticipantToCampaignResponse> handelAddParticipantToCampaign(@RequestBody AddParticipantToCampaign addParticipantToCampaign){

        return  campaignParticipantService.TestAddCampaignParticipant(addParticipantToCampaign);

    }
}
