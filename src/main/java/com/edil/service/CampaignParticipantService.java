package com.edil.service;



import com.edil.domain.Account;
import com.edil.domain.Campaign;
import com.edil.domain.Receipt;
import com.edil.domain.enums.CampaignStatus;
import com.edil.dto.internal.CbePayload;
import com.edil.dto.request.AddParticipantToCampaignRequest;
import com.edil.dto.response.AddParticipantToCampaignResponse;
import com.edil.dto.response.UserMeResponse;
import com.edil.repository.CampaignParticipantsRepository;
import com.edil.repository.ReceiptRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CampaignParticipantService {

    private  final  CampaignParticipantServiceUtil campaignParticipantServiceUtil;
    private  final UserService userService;
    private  final CampaignParticipantsRepository campaignParticipantsRepository;
    private  final  CampaignService campaignService;
    private  final ReceiptRepository receiptRepository;

    CampaignParticipantService(CampaignParticipantServiceUtil campaignParticipantServiceUtil, UserService userService, CampaignParticipantsRepository campaignParticipantsRepository, CampaignService campaignService,
                               ReceiptRepository receiptRepository){
        this.campaignParticipantServiceUtil = campaignParticipantServiceUtil;
        this.userService = userService;
        this.campaignParticipantsRepository = campaignParticipantsRepository;
        this.campaignService = campaignService;
        this.receiptRepository = receiptRepository;
    }






    @Transactional
    public ResponseEntity<AddParticipantToCampaignResponse> AddCampaignParticipant(AddParticipantToCampaignRequest addParticipantToCampaignRequest, String userEmail){
        // lets first make sure the user is active and make sure is not part of the campain already.

        Account biengAddedUserAccount = userService.getUserByEmail(userEmail);
        if(!biengAddedUserAccount.getIsActive()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("user is banned"));
        }

        if(campaignParticipantsRepository.existsByAccountIdAndCampaignId(biengAddedUserAccount.getId(), addParticipantToCampaignRequest.campaignId())){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("user already joined campaign"));
        }

        Campaign campaign = campaignService.getCampaignById(addParticipantToCampaignRequest.campaignId());

        // now the logic starts here
        if(!campaign.getStatus().equals(CampaignStatus.APPROVED)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("campaign is over/or doesn't exist anymore"));
        }

        CbePayload cbePayload = campaignParticipantServiceUtil.fetchCbePayload(addParticipantToCampaignRequest.fullPaymentLink());

        if (cbePayload == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("The link provided does't match with the registered CBE API"));
        }

        if (!cbePayload.status().equals("COMPLETED")){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("The link provided is invalid, input a correct one"));
        }

        // Campaign data

        LocalDateTime campaignEndDate = campaign.getEndDate();
        LocalDateTime campaignStratDate = campaign.getStartDate();
        int targetEntries = campaign.getTargetEntries();
        int currentEntries = campaign.getJoinedUsers();
        BigDecimal ticketPrice  = campaign.getTicketPrice();
        String receiverAccountNumber = campaign.getCreator().getCreatorProfile().getPayoutBankAccount();
        String receiverAccountName = campaign.getCreator().getCreatorProfile().getFullName();


        // Data Extracted from CBE payload

        String receiverAccountNumberFromReceipt = cbePayload.creditAccountNo();
        String receiverAccountNameFromReceipt = cbePayload.creditAccountHolder();

        BigDecimal receivedAmountFromRecept = BigDecimal.valueOf(Integer.valueOf(cbePayload.debitAmount()).doubleValue()); // YEAH keep an eye on this mess
        LocalDateTime transactionMadelocalDateTime = LocalDateTime.ofInstant(cbePayload.dateTimes(), ZoneId.systemDefault()); // the instant mapping might have failed so check that as well

        Pattern extractTheLastFourNumbersFromTheAccountNumber  = Pattern.compile("(.{4})$");
        Matcher matcherForAccountNumber = extractTheLastFourNumbersFromTheAccountNumber.matcher(receiverAccountNumber);
        Matcher matcherForReceiptAccountNumber = extractTheLastFourNumbersFromTheAccountNumber.matcher(receiverAccountNumberFromReceipt);
        String extractedFourLastDigitsFromAccountNumber = matcherForAccountNumber.group(1);
        String extractedFouLastDigitsFromReceiptAccountNumber = matcherForReceiptAccountNumber.group(1);

        // edn cbe payload

        if(receiptRepository.existsReceiptById(cbePayload.v2Key())){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("this link is already registered/used"));

        }



        if (!extractedFouLastDigitsFromReceiptAccountNumber.equals(extractedFourLastDigitsFromAccountNumber ) || !receiverAccountName.equals(receiverAccountNameFromReceipt)){

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("the account you made payment information to does't match the provided account's information"));


        }

        if(transactionMadelocalDateTime.isAfter(campaignEndDate) || transactionMadelocalDateTime.isBefore(campaignStratDate)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("the date on the receipt is not valid"));
        }

        // well after this we have asert that the payment is made lets update it well let me do it tommorow i guess.






























        log.info("succsessfully fetched recipt");
        log.info("to maeke sure i got it {}", cbePayload.debitAccountHolder());
        log.info("{}", cbePayload);


        return ResponseEntity.status(HttpStatus.OK).build();
    }




    public ResponseEntity<AddParticipantToCampaignResponse> TestAddCampaignParticipant(AddParticipantToCampaignRequest addParticipantToCampaignRequest){

        CbePayload cbePayload = campaignParticipantServiceUtil.fetchCbePayload(addParticipantToCampaignRequest.fullPaymentLink());

        if (cbePayload == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("The link provided does't match with the registered CBE API"));
        }

        if (!cbePayload.status().equals("COMPLETED")){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("The link provided is invalid, input a correct one"));
        }

        log.info("succsessfully fetched recipt");
        log.info("to maeke sure i got it {}", cbePayload.debitAccountHolder());
        log.info("{}", cbePayload);


        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
