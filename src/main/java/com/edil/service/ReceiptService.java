package com.edil.service;


import com.edil.dto.request.HasReceiptBeenUsedBeforeRequest;
import com.edil.dto.response.HasReceiptBeenUsedBeforeResponse;
import com.edil.dto.response.UploadReceiptResponse;
import com.edil.repository.ReceiptRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {
    private final ReceiptRepository receiptRepository;
    private final Cache<UUID, Boolean> reciptUploadKeyCache;


    public ResponseEntity<HasReceiptBeenUsedBeforeResponse> checkForReceiptExitance(HasReceiptBeenUsedBeforeRequest hasReceiptBeenUsedBeforeRequest){
        String url = hasReceiptBeenUsedBeforeRequest.paymentUrl();

        String patternTobeMatched = "^https://mbreciept\\.cbe\\.com\\.et/v2-([a-zA-Z0-9]+)$";
        Pattern pattern = Pattern.compile(patternTobeMatched);
        Matcher matcher = pattern.matcher(url);

        if (!matcher.matches()) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new HasReceiptBeenUsedBeforeResponse(null, false));
        }

        String extractedUniqueV2Key = matcher.group(1);

        if(receiptRepository.existsReceiptById(extractedUniqueV2Key)){

            return ResponseEntity.status(HttpStatus.OK).body(new HasReceiptBeenUsedBeforeResponse(true,  true));

        }

        return ResponseEntity.status(HttpStatus.OK).body(new HasReceiptBeenUsedBeforeResponse(false,  true));

    }


    public HasReceiptBeenUsedBeforeResponse checkForReceiptExitance(String url){


        String patternTobeMatched = "^https://mbreciept\\.cbe\\.com\\.et/v2-([a-zA-Z0-9]+)$";
        Pattern pattern = Pattern.compile(patternTobeMatched);
        Matcher matcher = pattern.matcher(url);

        if (!matcher.matches()) {
            return new HasReceiptBeenUsedBeforeResponse(null, false);
        }

        String extractedUniqueV2Key = matcher.group(1);

        if(receiptRepository.existsReceiptById(extractedUniqueV2Key)){

            return (new HasReceiptBeenUsedBeforeResponse(true,  true));

        }

        return (new HasReceiptBeenUsedBeforeResponse(false,  true));

    }

    public ResponseEntity<UploadReceiptResponse> createUploadReceiptKey(){
        UUID randomUUId = UUID.randomUUID();
        reciptUploadKeyCache.put(randomUUId, true);

        return ResponseEntity.status(HttpStatus.OK).body(new UploadReceiptResponse(randomUUId));
    }

    public ResponseEntity<Void> checkForUploadReceiptKey(String uri){
        String[] splittedUri= uri.split("/");
        //""/upload/receipt/receiptKey
        //0/1/2/3

        if (splittedUri.length!=4 ){

            return  ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }
        try {
            UUID receiptUploadKey = UUID.fromString(splittedUri[3]);

        }

        catch (Exception exception){
            log.info("the recipt key extracted can't be mapped to UUID: " + splittedUri[3]);
            return   ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID receiptUploadKey = UUID.fromString(splittedUri[3]);

        Map<UUID,Boolean> uuidBooleanMap = reciptUploadKeyCache.asMap();




        log.info("does the uuidBoolmap conaintein the key {} answer {}", receiptUploadKey, uuidBooleanMap.containsKey(receiptUploadKey));
        if (!uuidBooleanMap.containsKey(receiptUploadKey)){


            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        }

        if (!uuidBooleanMap.get(receiptUploadKey)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }


        log.info("we are returning ok");

        return  ResponseEntity.status(HttpStatus.OK).build();
    }



}
