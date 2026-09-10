package com.edil.controller;


import com.edil.dto.request.HandlePetitionRequest;
import com.edil.dto.request.ResolvePetitionRequest;
import com.edil.dto.response.GetPetitionsBeingHandledByMeResponse;
import com.edil.dto.response.GetUnresolvedPetitionsResponse;
import com.edil.dto.response.HandlePetitionResponse;
import com.edil.dto.response.ResolvePetitionResponse;
import com.edil.service.PetitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/petition")
public class PetitionController {
    private final PetitionService petitionService;


    @GetMapping("/unresolved")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<Page<GetUnresolvedPetitionsResponse>> HandelGetUnresolvedPetitions(Pageable pageable){

        return petitionService.getUnresolvedPetitions(pageable);

    }

    @PostMapping("/handle")
    @PreAuthorize("hasRole('ADMIN')")

    public  ResponseEntity<HandlePetitionResponse> handlePetitionController(@RequestBody HandlePetitionRequest handlePetitionRequest, @AuthenticationPrincipal String email){
        return petitionService.handlePetition(handlePetitionRequest, email);
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<ResolvePetitionResponse> handleResolvePetition(@RequestBody @Validated ResolvePetitionRequest resolvePetitionRequest, @AuthenticationPrincipal String email){


        log.info("me the petition end point is being hit");

        return petitionService.resolvePetition(resolvePetitionRequest, email);

    }

    @GetMapping("/admin/being/handled")
    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT_ADMIN')")

    public ResponseEntity<Page<GetPetitionsBeingHandledByMeResponse>> handelGetPetitionsIAmHandling(@AuthenticationPrincipal String email, Pageable pageable){
        return  petitionService.getBeingHandledPetitionsByAdmin(pageable, email);
    }

}
