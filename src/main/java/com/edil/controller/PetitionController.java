package com.edil.controller;


import com.edil.dto.response.GetUnresolvedPetitionsResponse;
import com.edil.service.PetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
