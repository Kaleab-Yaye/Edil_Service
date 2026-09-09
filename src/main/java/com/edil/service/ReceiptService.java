package com.edil.service;


import com.edil.dto.request.HasReceiptBeenUsedBeforeRequest;
import com.edil.dto.response.HasReceiptBeenUsedBeforeResponse;
import com.edil.repository.ReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReceiptService {
    private final ReceiptRepository receiptRepository;

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


}
