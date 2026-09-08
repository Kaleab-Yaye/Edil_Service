package com.edil.dto.request;

import jakarta.validation.constraints.NotNull;

public record HasReceiptBeenUsedBeforeRequest (
        @NotNull String paymentUrl
){
}
