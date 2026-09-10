package com.edil.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;



public record AddParticipantFromOpenResponse(
        String message,
        String userFullName,
        UUID campaignId,
        String campaignName,
        BigDecimal amountPayed,
        String receiptLink,
        String edilNumber,
        String generateEmail,
        String generatePassword,
        String creatorsFullName,
        LocalDateTime localDateTime

)  {

    public static AddParticipantFromOpenResponse getAddParticipantFromOpenResponseWithOnlyMessage(String message){

        return  new AddParticipantFromOpenResponse(message, null, null, null, null, null, null, null, null, null, null);
    }

}
