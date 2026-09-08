package com.edil.controller;


import com.edil.dto.request.HasReceiptBeenUsedBeforeRequest;
import com.edil.dto.response.HasReceiptBeenUsedBeforeResponse;
import com.edil.service.ReceiptService;
import com.fasterxml.classmate.members.ResolvedParameterizedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/receipt")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/check/use")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<HasReceiptBeenUsedBeforeResponse> handleReceiptUsageCheck(@RequestBody HasReceiptBeenUsedBeforeRequest hasReceiptBeenUsedBeforeRequest){
        return receiptService.checkForReceiptExitance(hasReceiptBeenUsedBeforeRequest);
    }

}
