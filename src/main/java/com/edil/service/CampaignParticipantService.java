package com.edil.service;



import com.edil.dto.internal.CbePayload;
import com.edil.dto.request.AddParticipantToCampaign;
import com.edil.dto.response.AddParticipantToCampaignResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CampaignParticipantService {

    private  final  CampaignParticipantServiceUtil campaignParticipantServiceUtil;

    CampaignParticipantService(CampaignParticipantServiceUtil campaignParticipantServiceUtil){
        this.campaignParticipantServiceUtil = campaignParticipantServiceUtil;
    }

    public ResponseEntity<AddParticipantToCampaignResponse> TestAddCampaignParticipant(AddParticipantToCampaign addParticipantToCampaign){

        CbePayload cbePayload = campaignParticipantServiceUtil.fetchCbePayload(addParticipantToCampaign.fullPaymentLink());

        if (cbePayload == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("The link provided does't match with the registered CBE API"));
        }

        if (!cbePayload.status().equals("COMPLETED")){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("The link provided is invalid, input a correct one"));
        }

        log.info("succsessfully fetched recipt");
        log.info("to maeke sure i got it {}", cbePayload.debitAccountHolder());
        log.info("{}", cbePayload);


        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
