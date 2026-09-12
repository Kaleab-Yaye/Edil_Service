package com.edil.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;


@RequiredArgsConstructor

public class AddParticipantFromOpenResponse


  {

    String message;
    String userFullName;
    UUID campaignId;
    String campaignName;
    BigDecimal amountPayed;
    String receiptLink;
    String edilNumber;
    String generateEmail;
    String generatePassword;
    String creatorsFullName;
    LocalDateTime localDateTime;
    Boolean reuploadReceipt = false;


      public AddParticipantFromOpenResponse(
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
      ) {
          this.message = message;
          this.userFullName = userFullName;
          this.campaignId = campaignId;
          this.campaignName = campaignName;
          this.amountPayed = amountPayed;
          this.receiptLink = receiptLink;
          this.edilNumber = edilNumber;
          this.generateEmail = generateEmail;
          this.generatePassword = generatePassword;
          this.creatorsFullName = creatorsFullName;
          this.localDateTime = localDateTime;
      }


      public AddParticipantFromOpenResponse(
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
              LocalDateTime localDateTime,
              Boolean reaUploadReceipt
      ) {
          this.message = message;
          this.userFullName = userFullName;
          this.campaignId = campaignId;
          this.campaignName = campaignName;
          this.amountPayed = amountPayed;
          this.receiptLink = receiptLink;
          this.edilNumber = edilNumber;
          this.generateEmail = generateEmail;
          this.generatePassword = generatePassword;
          this.creatorsFullName = creatorsFullName;
          this.localDateTime = localDateTime;
          this.reuploadReceipt = reaUploadReceipt;
      }




    public static AddParticipantFromOpenResponse getAddParticipantFromOpenResponseWithOnlyMessage(String message){

        return  new AddParticipantFromOpenResponse(message, null, null, null, null, null, null, null, null, null, null, null);
    }

      public static AddParticipantFromOpenResponse getAddParticipantFromOpenResponseWithMessageAndUploadAgainFlag(String message){

          return  new AddParticipantFromOpenResponse(message, null, null, null, null, null, null, null, null, null, null, true);
      }

}
