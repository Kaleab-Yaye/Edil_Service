package com.edil.controller;


import com.edil.dto.request.*;
import com.edil.dto.response.*;
import com.edil.service.CampaignParticipantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerTypePredicate;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/participant")
@RequiredArgsConstructor// /api/v1/participant/public/add/participant
public class CampaignParticipantController {
    private final CampaignParticipantService campaignParticipantService;
    private final ObjectMapper objectMapper;
    private final Validator validator;


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

    @GetMapping("/cancel/slot")
    public ResponseEntity<HttpStatus> cancelOPenSlot(@AuthenticationPrincipal String email){

        return campaignParticipantService.cancelReservedSlot(email);

    }

    @PostMapping("/submit/petition")
    public ResponseEntity<CreatePetitionResponse> createPetitionHandler(@RequestBody CreatePetitionRequest createPetitionRequest, @AuthenticationPrincipal String email){
        return  campaignParticipantService.submitPetition(createPetitionRequest, email);
    }


    @PostMapping("admin/add/participant")
    @PreAuthorize("hasAnyRole('ADMIN','ROOT_ADMIN')")
    public ResponseEntity<AddUserTOCampaignByAdminResponse> addUserTOCampaignByAdminController(@RequestBody AddUserTOCampaignByAdminRequest addUserTOCampaignByAdminRequest, @AuthenticationPrincipal String email){
        return  campaignParticipantService.addUserTOCampaignByAdmin(addUserTOCampaignByAdminRequest, email);
    }

//    @PostMapping("public/add/participant")
//    @PermitAll
//    public ResponseEntity<AddParticipantFromOpenResponse> addParticipantsFromOpen(@RequestBody @Validated AddParticipantToCampaignFromOpenWithLinkRequest addParticipantToCampaignFromOpenWithLinkRequest){
//        return campaignParticipantService.addCampaignParticipantFromPublic(addParticipantToCampaignFromOpenWithLinkRequest);
//    }

    @PostMapping("/public/add/participant")
    public ResponseEntity<AddParticipantFromOpenResponse> addParticipantsFromOpen(@RequestBody JsonNode jsonNode, @RequestParam("format") String format) throws Exception{
        if(format.equalsIgnoreCase("link")){

            AddParticipantToCampaignFromOpenWithLinkRequest addParticipantToCampaignFromOpenWithLinkRequest =  objectMapper.treeToValue(jsonNode, AddParticipantToCampaignFromOpenWithLinkRequest.class );
            validateObject(addParticipantToCampaignFromOpenWithLinkRequest);

            log.info("the validation of the object has passed and have reached here");

            return campaignParticipantService.addCampaignParticipantFromPublic(addParticipantToCampaignFromOpenWithLinkRequest);


        }
        if(format.equalsIgnoreCase("image")){

            AddParticipantToCampaignFromOpenWithImageRequest addParticipantRequest =  objectMapper.treeToValue(jsonNode, AddParticipantToCampaignFromOpenWithImageRequest.class );
            validateObject(addParticipantRequest);
            return campaignParticipantService.addCampaignParticipantFromPublicWithReceiptImage(addParticipantRequest);

        }
        return  ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    private <T> void validateObject(T objectToValidate) {

        Set<ConstraintViolation<T>> violations = validator.validate(objectToValidate);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}










