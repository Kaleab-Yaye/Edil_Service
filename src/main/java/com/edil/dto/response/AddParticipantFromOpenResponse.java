package com.edil.dto.response;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

public record AddParticipantFromOpenResponse(
        String message,
        String userFullName,
        UUID campaignId,
        String campaignName,
        BigInteger amountPayed,
        String receiptLink,
        String participationNumber,
        String generateEmail,
        String generatePassword,
        LocalDateTime localDateTime

)  {
}
