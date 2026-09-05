package com.edil.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FetchOnGoingSlotInformationForUserResponse(
        boolean hasReservedSlot,
        Long leftTimeInSeconds,
        UUID slotKey,
        UUID campaignId,
        String accountNumber,
        BigDecimal amountTOPay,
        String nameOfAccountHolder
) {
}
