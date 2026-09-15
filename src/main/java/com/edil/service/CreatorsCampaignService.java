package com.edil.service;


import com.edil.domain.Account;
import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipantsPdf;
import com.edil.domain.enums.CampaignStatus;
import com.edil.dto.request.EndCampaignByCreatorRequest;
import com.edil.dto.request.GetPdfInfoRequest;
import com.edil.dto.response.EndCampaignByCreatorResponse;
import com.edil.dto.response.GetPdfInfoResponse;
import com.edil.exception.CampaignNotFoundException;
import com.edil.repository.CampaignRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreatorsCampaignService {

    private final CampaignRepository campaignRepository;
    private final UserService userService;



    public ResponseEntity<EndCampaignByCreatorResponse> endCampaignByCreator(EndCampaignByCreatorRequest endCampaignByCreatorRequest, String email ){

        Campaign campaign= campaignRepository.getCampaignsById(endCampaignByCreatorRequest.campaignId()).orElseThrow(()->new CampaignNotFoundException(endCampaignByCreatorRequest.campaignId().toString()));
        Account account = userService.getAccountByEmail(email);
        if (!campaign.getCreator().getId().equals(account.getId())){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new EndCampaignByCreatorResponse("you are not allowed to change state for this campaign"));
        }


        if (!campaign.getStatus().equals(CampaignStatus.APPROVED)){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new EndCampaignByCreatorResponse("the current status of campaign can't be ended"));
        }

        campaign.setStatus(CampaignStatus.ENDED_BY_CREATOR);
        campaignRepository.save(campaign);




        return  ResponseEntity.status(HttpStatus.OK).body(new EndCampaignByCreatorResponse("the status will be updated, give it a second"));
    }

    public ResponseEntity<GetPdfInfoResponse> getPdfForCreator(GetPdfInfoRequest getPdfInfoRequest, String email){
        Campaign campaign = campaignRepository.getCampaignsById(getPdfInfoRequest.campaignId()).orElseThrow(()->new CampaignNotFoundException(getPdfInfoRequest.campaignId().toString()));
        if(!campaign.getCreator().getEmail().equals(email)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if(!campaign.getStatus().equals(CampaignStatus.ENDED)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if(!campaign.getHasPdf()){
            return ResponseEntity.status(HttpStatus.OK).body(new GetPdfInfoResponse(null, null, true));
        }

        CampaignParticipantsPdf campaignParticipantsPdf = campaign.getCampaignParticipantsPdf();

        return  ResponseEntity.status(HttpStatus.OK).body(new GetPdfInfoResponse(campaignParticipantsPdf.getPdfName(), campaignParticipantsPdf.getPdfSizeInBytes(), true));

    }
}
