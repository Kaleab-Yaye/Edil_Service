package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HasReceiptBeenUsedBeforeRequest (
        @NotBlank String paymentUrl
){
}
