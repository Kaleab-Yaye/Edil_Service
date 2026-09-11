package com.edil.controller;


import com.edil.dto.request.HasReceiptBeenUsedBeforeRequest;
import com.edil.dto.response.HasReceiptBeenUsedBeforeResponse;
import com.edil.dto.response.UploadReceiptResponse;
import com.edil.service.ReceiptService;
import com.fasterxml.classmate.members.ResolvedParameterizedMember;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
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

    @GetMapping("/create/upload/key")
    public ResponseEntity<UploadReceiptResponse> handleCreateReceiptUploadKey (){
        return  receiptService.createUploadReceiptKey();
    }

    @GetMapping("/check/upload/auth")
    public ResponseEntity<Void> handelReceiptUploadAuth(HttpServletRequest httpServletRequest){
        String uri = httpServletRequest.getHeader("X-Original-URI");
        log.info("the received uri for auth is {}", uri);

        return  receiptService.checkForUploadReceiptKey(uri);
    }


}
