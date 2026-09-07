package com.edil.service;



import com.edil.config.util.StoreCampaignToSlotHashMap;
import com.edil.domain.*;
import com.edil.domain.enums.CampaignStatus;
import com.edil.domain.enums.PetitionStatus;
import com.edil.dto.internal.CbePayload;
import com.edil.dto.internal.SlotKeyToCampaignAndUserIdDto;
import com.edil.dto.request.*;
import com.edil.dto.response.*;
import com.edil.repository.*;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.patterns.ConcreteCflowPointcut;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.StyledEditorKit;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignParticipantService {

    private final CampaignParticipantServiceUtil campaignParticipantServiceUtil;
    private final UserService userService;
    private final CampaignParticipantsRepository campaignParticipantsRepository;
    private final CampaignService campaignService;
    private final ReceiptRepository receiptRepository;
    private final SlotRepository slotRepository;
    private  final Cache<UUID, SlotKeyToCampaignAndUserIdDto> slotKeyToCampaignIdCache;
    private final Cache<String, UUID> userEmailToSlotAvailableCheckCache;

    private final ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepository;
    private  final PetitionService petitionService;





    @Transactional
    public ResponseEntity<AddParticipantToCampaignResponse> AddCampaignParticipant(AddParticipantToCampaignRequest addParticipantToCampaignRequest, String userEmail) {



            // lets first make sure the user is active and make sure is not part of the campain already.

            Account biengAddedUserAccount = userService.getUserByEmail(userEmail);
            if (!biengAddedUserAccount.getIsActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("user is banned"));
            }


            if (campaignParticipantsRepository.existsByAccountIdAndCampaignId(biengAddedUserAccount.getId(), addParticipantToCampaignRequest.campaignId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("user already joined campaign"));
            }

            //protection for evicted email->key mapping
            if(!userEmailToSlotAvailableCheckCache.asMap().containsKey(userEmail)){
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("user has no slot reserved"));
            }


            if (!slotKeyToCampaignIdCache.asMap().containsKey(addParticipantToCampaignRequest.slotKey())) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("slot key has expired or is not valid"));

            }

            if (!slotKeyToCampaignIdCache.asMap().get(addParticipantToCampaignRequest.slotKey()).userEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("using an invalid slot id"));
            }


            if (!slotKeyToCampaignIdCache.asMap().get(addParticipantToCampaignRequest.slotKey()).campaignId().equals(addParticipantToCampaignRequest.campaignId())) { // has to be fixed assap
                log.info("the value in the map for the key {} is {} and the campaign id that came ver the reqeust is {}", addParticipantToCampaignRequest.slotKey(), slotKeyToCampaignIdCache.asMap().get(addParticipantToCampaignRequest.slotKey()), addParticipantToCampaignRequest.campaignId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("slot key has expired or is not valid"));
            }

            Campaign campaign = campaignService.getCampaignById(addParticipantToCampaignRequest.campaignId());

            // now the logic starts here
            if (!campaign.getStatus().equals(CampaignStatus.APPROVED)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("campaign is over/or doesn't exist anymore"));
            }

            CbePayload cbePayload = campaignParticipantServiceUtil.fetchCbePayload(addParticipantToCampaignRequest.fullPaymentLink());

            if (cbePayload == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("The link provided does't match with the registered CBE API"));
            }

            if (!cbePayload.status().equals("COMPLETED")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("The link provided is invalid, input a correct one"));
            }

            // Campaign data

            LocalDateTime campaignEndDate = campaign.getEndDate();
            LocalDateTime campaignStratDate = campaign.getStartDate();
            int targetEntries = campaign.getTargetEntries();
            int currentEntries = campaign.getJoinedUsers();
            BigDecimal ticketPrice = campaign.getTicketPrice();
            String receiverAccountNumber = campaign.getCreator().getCreatorProfile().getPayoutBankAccount();
            String receiverAccountName = campaign.getCreator().getCreatorProfile().getFullName();


            // Data Extracted from CBE payload

            String receiverAccountNumberFromReceipt = cbePayload.creditAccountNo();
            String receiverAccountNameFromReceipt = cbePayload.creditAccountHolder();

            BigDecimal receivedAmountFromRecept = cbePayload.debitAmount(); // YEAH keep an eye on this mess
            LocalDateTime transactionMadelocalDateTime = LocalDateTime.ofInstant(cbePayload.dateTimes()[0], ZoneId.systemDefault()); // the instant mapping might have failed so check that as well

            Pattern extractTheLastFourNumbersFromTheAccountNumber = Pattern.compile("^.*(.{4})$");
            log.info("the account number from campaign is {}", receiverAccountNumber);
            log.info("the account number from receipt is {}", receiverAccountNumberFromReceipt);


            Matcher matcherForAccountNumber = extractTheLastFourNumbersFromTheAccountNumber.matcher(receiverAccountNumber);
            Matcher matcherForReceiptAccountNumber = extractTheLastFourNumbersFromTheAccountNumber.matcher(receiverAccountNumberFromReceipt);
            String extractedFourLastDigitsFromAccountNumber = "";
            String extractedFouLastDigitsFromReceiptAccountNumber = "";

            if (matcherForAccountNumber.matches()) {
                extractedFourLastDigitsFromAccountNumber = matcherForAccountNumber.group(1);

            }

            if (matcherForReceiptAccountNumber.matches()) {

                extractedFouLastDigitsFromReceiptAccountNumber = matcherForReceiptAccountNumber.group(1);

            }


            // edn cbe payload

            if (receiptRepository.existsReceiptById(cbePayload.v2Key())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("this link is already registered/used"));

            }


            if (!extractedFouLastDigitsFromReceiptAccountNumber.equals(extractedFourLastDigitsFromAccountNumber) || !receiverAccountName.equals(receiverAccountNameFromReceipt)) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("the account you made payment information to does't match the provided account's information"));


            }

            if (transactionMadelocalDateTime.isAfter(campaignEndDate) || transactionMadelocalDateTime.isBefore(campaignStratDate)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse("the date on the receipt is not valid"));
            }

            log.info("the rpice from the campaign listing is {}", ticketPrice);
            log.info("the price from the receipt is {}", receivedAmountFromRecept);

            if (receivedAmountFromRecept.compareTo(campaign.getTicketPrice()) < 0) {

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AddParticipantToCampaignResponse(" the amount payed is less than the ticket price"));
            }

            // well after this we have asert that the payment is made lets update it well let me do it tommorow i guess.

            CampaignParticipants campaignParticipants = new CampaignParticipants();
            campaignParticipants.setCampaign(campaign);
            campaignParticipants.setAccount(biengAddedUserAccount);
            campaignParticipants.setReceiptHash(cbePayload.v2Key());
            campaignParticipantsRepository.save(campaignParticipants);

            // now update the add player count, and mark the campaign as over if it goes above the limit


            // update the the receipt table

            Receipt receipt = new Receipt();
            receipt.setId(cbePayload.v2Key());
            receipt.setReferenceNumber(cbePayload.id());

            receiptRepository.save(receipt);

            // had to invalidate the cache

            slotKeyToCampaignIdCache.invalidate(addParticipantToCampaignRequest.slotKey());

            return ResponseEntity.status(HttpStatus.OK).build();

        }





    public ResponseEntity<CanParticipantJoinCampaignResponse> canParticipantJoinCampaign(CanParticipantJoinCampaignRequest request, String email){

        UUID userUUId   = userService.getUserUUIDByEmail(email);
        UserProfile userProfile = userService.getUserByEmail(email).getUserProfile();

        if (campaignParticipantsRepository.existsByAccountIdAndCampaignId(userUUId, request.campaignId())){
            return  ResponseEntity.status(HttpStatus.CONFLICT).body(new CanParticipantJoinCampaignResponse(false, false, null, null, false));
        }

        UUID  slotKeyFromEmailToBooleanCache = userEmailToSlotAvailableCheckCache.getIfPresent(email);

        if(slotKeyFromEmailToBooleanCache != null){
         SlotKeyToCampaignAndUserIdDto  slotKeyToCampaignAndUserIdDto =  slotKeyToCampaignIdCache.getIfPresent(slotKeyFromEmailToBooleanCache);

         if(slotKeyToCampaignAndUserIdDto == null){
             log.warn("has the user resvered a slot check returend true but can get the key");
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

         }
            return  ResponseEntity.status(HttpStatus.CONFLICT).body(new CanParticipantJoinCampaignResponse(false, true, slotKeyToCampaignAndUserIdDto.campaignId(), slotKeyFromEmailToBooleanCache, false ));
        }

        if(petitionService.doesUserHasOngoingPetitionForCampaign(request.campaignId(), userProfile.getId())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new CanParticipantJoinCampaignResponse(false,false,null,null,true));
        }


        return ResponseEntity.status(HttpStatus.OK).body(new CanParticipantJoinCampaignResponse(true, false, null, null, false));
    }

    public boolean canParticipantJoinCampaign(UUID campaignId, String email){

        UUID userUUId   = userService.getUserUUIDByEmail(email);

        return !campaignParticipantsRepository.existsByAccountIdAndCampaignId(userUUId, campaignId);
    }


    public ResponseEntity<CreateCampaignSlotForUserResponse> createCampaignSlotForUser(CreateCampaignSlotForUserRequest request, String userEmail){

        Campaign campaign = campaignService.getCampaignById(request.campaignId());
        UUID userId = userService.getUserUUIDByEmail(userEmail);

        if(campaign.getStatus()!=CampaignStatus.APPROVED ||  !canParticipantJoinCampaign(request.campaignId(), userEmail) ){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
        }

        // has to write concurent safe excution for this


        if(userEmailToSlotAvailableCheckCache.asMap().containsKey(userEmail)){
            return  ResponseEntity.status(HttpStatus.CONFLICT).body(new CreateCampaignSlotForUserResponse(null, false, null, null, null));
        }


        while(true) {

            int expectedValue =StoreCampaignToSlotHashMap.campaignToSlotStore.get(request.campaignId()).intValue();

            if ( expectedValue<= 0) {

                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new CreateCampaignSlotForUserResponse(null, false, null, null, null));

            }

            if ( StoreCampaignToSlotHashMap.campaignToSlotStore.get(request.campaignId()).compareAndSet(expectedValue, expectedValue-1)){
                break;
            }

        }

        Slot slot = new Slot();
        slot.setCampaignId(request.campaignId());
        slot.setUserEmail(userEmail);
        slotRepository.save(slot);


        log.info("putting the slot key in cache {}", slot.getId());

        slotKeyToCampaignIdCache.put(slot.getId(), SlotKeyToCampaignAndUserIdDto.returnSlotKeyToCampaignAndUserIdDtoWithTime( request.campaignId(), userEmail));
        userEmailToSlotAvailableCheckCache.put(userEmail, slot.getId());

        log.info("accessinng put slot for the key {} from the cache and the contains request is this {}", slot.getId(), slotKeyToCampaignIdCache.asMap().containsKey(slot.getId()));




        CreatorProfile accountHolder = campaign.getCreator().getCreatorProfile();

        return ResponseEntity.status(HttpStatus.OK).body(new CreateCampaignSlotForUserResponse(slot.getId(), true, accountHolder.getPayoutBankAccount(),campaign.getTicketPrice(), accountHolder.getFullName()));

    }

    public ResponseEntity<FetchOnGoingSlotInformationForUserResponse> fetchOngoingUserSlotInfo(String email){

        UUID slotKeyFromEmailToSlotKeyCache = userEmailToSlotAvailableCheckCache.getIfPresent(email);

        if(slotKeyFromEmailToSlotKeyCache==null){



            return ResponseEntity.status(HttpStatus.OK).body(


                    new FetchOnGoingSlotInformationForUserResponse(false , null, null, null , null, null, null)
            );
        }

        SlotKeyToCampaignAndUserIdDto slotKeyToCampaignAndUserIdDto  = slotKeyToCampaignIdCache.getIfPresent(slotKeyFromEmailToSlotKeyCache);
        if(slotKeyToCampaignAndUserIdDto==null){
            log.warn("user with the email adress {} a slot but can't find  the slot key stored", email);
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        long timeLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), slotKeyToCampaignAndUserIdDto.cachePutAt());
        Campaign campaign = campaignService.getCampaignById(slotKeyToCampaignAndUserIdDto.campaignId());

        return ResponseEntity.status(HttpStatus.OK).body(

                new FetchOnGoingSlotInformationForUserResponse(true, timeLeft, slotKeyFromEmailToSlotKeyCache, slotKeyToCampaignAndUserIdDto.campaignId(), campaign.getCreator().getCreatorProfile().getPayoutBankAccount(), campaign.getTicketPrice(), campaign.getCreator().getCreatorProfile().getFullName())
        );
    }

    public ResponseEntity<HttpStatus> cancelReservedSlot(String email){
        // so the thing if we invalidate the cache for the key to the sorted Campaign thingy. then it is going to be a mess since forced eviation will triger the payment complied path
        Map<String,UUID>  userEmailTOSlotKeyMap = userEmailToSlotAvailableCheckCache.asMap();
        if (!userEmailTOSlotKeyMap.containsKey(email)){
            return  ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        UUID slotKey = userEmailTOSlotKeyMap.get(email);
        Map<UUID, SlotKeyToCampaignAndUserIdDto> slotKeyToCampaignAndUserIdDtoMap = slotKeyToCampaignIdCache.asMap();
        if(!slotKeyToCampaignAndUserIdDtoMap.containsKey(slotKey)){
            log.warn(" a user was find in the email to key cache but the key was not on the cache email: {} key: {}", email, slotKey);
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        SlotKeyToCampaignAndUserIdDto slotKeyToCampaignAndUserIdDto = slotKeyToCampaignAndUserIdDtoMap.get(slotKey);
        SlotKeyToCampaignAndUserIdDto slotKeyToCampaignAndUserIdDtoFlaggedAsCancelled = slotKeyToCampaignAndUserIdDto.returnSlotKeyToCampaignAndUserIdDtoFlaggedAsReplaced();

        slotKeyToCampaignIdCache.put(userEmailToSlotAvailableCheckCache.getIfPresent(email), slotKeyToCampaignAndUserIdDtoFlaggedAsCancelled );

        return  ResponseEntity.status(HttpStatus.OK).build();



    }



    @Transactional
    public ResponseEntity<CreatePetitionResponse> submitPetition(CreatePetitionRequest createPetitionRequest, String  email){
        //first check if the user is in memory and has slot to submit so that we evict it.
        Map<String,UUID>  userEmailTOSlotKeyMap = userEmailToSlotAvailableCheckCache.asMap();
        UUID slotKey = null;
        SlotKeyToCampaignAndUserIdDto slotKeyToCampaignAndUserIdDto = null;
        SlotKeyToCampaignAndUserIdDto slotKeyToCampaignAndUserIdDtoFlaggedAsCancelled = null;

        if (userEmailTOSlotKeyMap.containsKey(email)){

            slotKey = userEmailTOSlotKeyMap.get(email);
        }

        Map<UUID, SlotKeyToCampaignAndUserIdDto> slotKeyToCampaignAndUserIdDtoMap = slotKeyToCampaignIdCache.asMap();
        if(slotKeyToCampaignAndUserIdDtoMap.containsKey(slotKey)){
            log.warn(" a user was find in the email to key cache but the key was not on the cache email: {} key: {}", email, slotKey);
            slotKeyToCampaignAndUserIdDto = slotKeyToCampaignAndUserIdDtoMap.get(slotKey);
            slotKeyToCampaignAndUserIdDtoFlaggedAsCancelled = slotKeyToCampaignAndUserIdDto.returnSlotKeyToCampaignAndUserIdDtoFlaggedAsReplaced();
        }

        // now we start the submiting of petition this should be handled by a peitition service and repo

        Campaign campaign = campaignService.getCampaignById(createPetitionRequest.campaignId());
        UserProfile userProfile = userService.getUserByEmail(email).getUserProfile();

        ResponseEntity<CreatePetitionResponse> responseResponseEntity = petitionService.submitPetition(createPetitionRequest, userProfile,  campaign);

        // evict if the user had any slot reserved
        slotKeyToCampaignIdCache.put(userEmailToSlotAvailableCheckCache.getIfPresent(email), slotKeyToCampaignAndUserIdDtoFlaggedAsCancelled );

        return  responseResponseEntity;


    }
}
