package com.edil.dto.response;

import java.math.BigDecimal;

public record GetCampaignPaymentInfoResponse (
        BigDecimal ticketPrice,
        String accountNumber,
        String accountHolderName
) {
}
