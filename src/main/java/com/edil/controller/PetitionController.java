package com.edil.controller;


import com.edil.dto.request.HandlePetitionRequest;
import com.edil.dto.request.ResolvePetitionRequest;
import com.edil.dto.response.GetUnresolvedPetitionsResponse;
import com.edil.dto.response.HandlePetitionResponse;
import com.edil.dto.response.ResolvePetitionResponse;
import com.edil.service.PetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/handel")
    @PreAuthorize("hasRole('ADMIN')")

    public  ResponseEntity<HandlePetitionResponse> handlePetitionController(@RequestBody HandlePetitionRequest handlePetitionRequest, @AuthenticationPrincipal String email){
        return petitionService.handlePetition(handlePetitionRequest, email);
    }

    @PostMapping("/resolve")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<ResolvePetitionResponse> handleResolvePetition(@RequestBody @Validated ResolvePetitionRequest resolvePetitionRequest, @AuthenticationPrincipal String email){

        return petitionService.resolvePetition(resolvePetitionRequest, email);

    }

}
