package com.edil.service;


import com.edil.domain.Campaign;
import com.edil.domain.Petition;
import com.edil.domain.UserProfile;
import com.edil.domain.enums.PetitionReason;
import com.edil.domain.enums.PetitionStatus;
import com.edil.dto.request.CreatePetitionRequest;
import com.edil.dto.response.CreatePetitionResponse;
import com.edil.repository.PetitionRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsPasswordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetitionService {

    private final PetitionRepository petitionRepository;
    private final ReactiveUserDetailsPasswordService reactiveUserDetailsPasswordService;



    @Transactional
   public ResponseEntity<CreatePetitionResponse> submitPetition(CreatePetitionRequest createPetitionRequest, UserProfile userProfile, Campaign campaign){
        if(petitionRepository.existsByPetitionerIdAndCampaignIdAndStatus(userProfile.getId(),createPetitionRequest.campaignId(), PetitionStatus.UNRESOLVED)){
            return  ResponseEntity.status(HttpStatus.CONFLICT).body(new CreatePetitionResponse("user already has ongoing petition for this campaign"));
        }

        Petition petition = Petition.builder().campaign(campaign).reason(createPetitionRequest.petitionReason()).
                paymentLink(createPetitionRequest.paymentLink())
                .petitioner(userProfile)
                .statement(createPetitionRequest.statement())
                .build();

        petitionRepository.save(petition);

        return  ResponseEntity.status(HttpStatus.OK).body(new CreatePetitionResponse("Petition Submitted"));


    }

    //true if it exists
    public boolean doesUserHasOngoingPetitionForCampaign(UUID campaignId, UUID  userProfileId ){
        return petitionRepository.existsByPetitionerIdAndCampaignIdAndStatus(campaignId, userProfileId, PetitionStatus.UNRESOLVED);
    }

}
