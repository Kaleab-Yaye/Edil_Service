package com.edil.dto.response;



public record HasReceiptBeenUsedBeforeResponse(
        Boolean hasBeenUsed,
        Boolean isValidUrl
) {
}
