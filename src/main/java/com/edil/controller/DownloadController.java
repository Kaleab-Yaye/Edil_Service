package com.edil.controller;


import com.edil.service.CreatorsCampaignService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/download")
public class DownloadController {

    private  final CreatorsCampaignService creatorsCampaignService;

    @GetMapping("/report/internal/auth") /// /api/v1/download/report/internal/auth

    public ResponseEntity<Void> downloadPdfAuthController(@RequestParam("key") UUID key,HttpServletRequest httpServletRequest ){


        log.info("being reached with the key value of {} and the raw url of {}", key, httpServletRequest.getHeader("X-Original-URI") );

        String rawUrl = httpServletRequest.getHeader("X-Original-URI");
        if (creatorsCampaignService.canDownloadPdf(key, rawUrl)){
            return  ResponseEntity.status(HttpStatus.OK).build();
        }
        return  ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

}
