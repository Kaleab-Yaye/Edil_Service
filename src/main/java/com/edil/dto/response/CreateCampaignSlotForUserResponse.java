package com.edil.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCampaignSlotForUserResponse(
        UUID slotKey,
        boolean slotAvailable,
        String accountNumber,
        BigDecimal amountTOPay,
        String nameOfAccountHolder

)
{
}
