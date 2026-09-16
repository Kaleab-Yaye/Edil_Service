package com.edil.service;


import com.edil.domain.Account;
import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipantsPdf;
import com.edil.domain.enums.CampaignStatus;
import com.edil.dto.request.EndCampaignByCreatorRequest;
import com.edil.dto.request.GetDownloadPdfKeyRequest;
import com.edil.dto.request.GetPdfInfoRequest;
import com.edil.dto.response.EndCampaignByCreatorResponse;
import com.edil.dto.response.GetDownloadPdfKeyResponse;
import com.edil.dto.response.GetPdfInfoResponse;
import com.edil.exception.CampaignNotFoundException;
import com.edil.repository.CampaignRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorsCampaignService {

    private final CampaignRepository campaignRepository;
    private final UserService userService;
    private final Cache<UUID,String> uploadPdfKeyToPdfNameCache;



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
    
   public ResponseEntity<GetDownloadPdfKeyResponse> getPdfDownloadKey(GetDownloadPdfKeyRequest getPdfDownloadRequest, String email){
        Campaign campaign = campaignRepository.getCampaignsById(getPdfDownloadRequest.campaignId()).orElseThrow(()->new CampaignNotFoundException(getPdfDownloadRequest.campaignId().toString()));
        if(!campaign.getCreator().getEmail().equals(email)){

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if(!campaign.getStatus().equals(CampaignStatus.ENDED)){

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if(!campaign.getHasPdf()){

            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }

        UUID key = UUID.randomUUID();
        String name = campaign.getCampaignParticipantsPdf().getPdfName();

        uploadPdfKeyToPdfNameCache.put(key, name);

        return  ResponseEntity.status(HttpStatus.OK).body(new GetDownloadPdfKeyResponse(key));
        
    }

    public boolean canDownloadPdf(UUID key, String rawRequestUrl){
       // ""/download/report/pdf/pdfname?key=uuid

        String[] brokenUrl = rawRequestUrl.split("/");
        String pdfName1 = brokenUrl[4];
        log.info("extracted name is {}", pdfName1);
        String[] pdfNameSecondArray = pdfName1.split("\\?");
        String pdfName = pdfNameSecondArray[0];
        log.info("extracted being compared name is {}",pdfName);

        String cachedPdfName = uploadPdfKeyToPdfNameCache.getIfPresent(key);
        if (cachedPdfName!=null){
            return cachedPdfName.equals(pdfName);
        }

        return false;

    }
}
