package com.edil.service;


import com.edil.domain.*;
import com.edil.domain.enums.AccountRole;
import com.edil.domain.enums.PetitionStatus;
import com.edil.dto.request.CreatePetitionRequest;
import com.edil.dto.request.HandlePetitionRequest;
import com.edil.dto.response.CreatePetitionResponse;
import com.edil.dto.response.GetUnresolvedPetitionsResponse;
import com.edil.dto.response.HandlePetitionResponse;
import com.edil.repository.AccountRepository;
import com.edil.repository.AdminProfileRepository;
import com.edil.repository.PetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PetitionService {

    private final PetitionRepository petitionRepository;
    private  final UserService userService;
    private  final AdminProfileRepository adminProfileRepository;



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

    public ResponseEntity<Page<GetUnresolvedPetitionsResponse>> getUnresolvedPetitions(Pageable pageable){

        Page<Petition> petitionPage = petitionRepository.getPetitionsByStatus(PetitionStatus.UNRESOLVED, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(petitionPage.map(this::mapUnresolvedPetitionToResponse));


    }


    private GetUnresolvedPetitionsResponse mapUnresolvedPetitionToResponse(Petition petition){

        return  GetUnresolvedPetitionsResponse.builder()
                .id(petition.getId())
                .petitionReason(petition.getReason())
                .createdAt(petition.getCreatedAt())
                .paymentLink(petition.getPaymentLink())
                .petitionerId(petition.getPetitioner().getId())
                .campaignId(petition.getCampaign().getId())
                .build();



    }


    @Transactional

   public ResponseEntity<HandlePetitionResponse> handlePetition(HandlePetitionRequest handlePetitionRequest, String email){

        Petition petition = petitionRepository.getPetitionsById(handlePetitionRequest.petitionId()).orElseThrow();
        if(!petition.getStatus().equals(PetitionStatus.UNRESOLVED)){
            return  ResponseEntity.status(HttpStatus.ALREADY_REPORTED).body(new HandlePetitionResponse(false));
        }


        Account account = userService.getAccountByEmail(email);

        if(!(account.getRole().equals(AccountRole.ADMIN)||account.getRole().equals(AccountRole.ROOT_ADMIN))){
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AdminProfile adminAccount = account.getAdminProfile();

        petition.setResolverAdmin(adminAccount);
        petition.setStatus(PetitionStatus.BING_HANDLED);


        petitionRepository.save(petition);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HandlePetitionResponse(true));


    }

}
