package com.edil.service;


import com.edil.domain.ArchivedCampaignParticipants;
import com.edil.domain.Campaign;
import com.edil.domain.CampaignParticipant;
import com.edil.domain.enums.CampaignStatus;
import com.edil.dto.internal.CbePayload;
import com.edil.exception.CBE5xxServerException;
import com.edil.repository.ArchivedCampaignParticipantsRepository;
import com.edil.repository.CampaignParticipantsRepository;
import com.edil.repository.CampaignRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.passay.data.EnglishCharacterData;
import org.passay.generate.PasswordGenerator;
import org.passay.rule.CharacterRule;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignParticipantServiceUtil {

    private final RestClient restClient;
    private  final CampaignParticipantsRepository campaignParticipantsRepository;
    private  final ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepository;
    private final CampaignRepository campaignRepository;
    private  final ObjectMapper objectMapper;



    public CbePayload fetchCbePayload(String rawUri) {

        //Stat-request header, that will be subject to a lot of change possibley
        final String x_app_id = "d1292e42-7400-49de-a2d3-9731caa4c819";
        final String x_app_version = "0a01980b-9859-1369-8198-59f403820000";
        final String user_agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36 Edg/152.0.0.0";
        final String Origin = "https://mbreciept.cbe.com.et";
        final String Referer = "https://mbreciept.cbe.com.et/";
        //End

        // the real end point :: "https://mb.cbe.com.et/api/v1/transactions/public/transaction-detail/v2-";
        String cbeJasonPayloadEndPoint = "http://localhost:3005/api/v1/transactions/public/transaction-detail/v2-";

        String patternTobeMatched = "^https://mbreciept\\.cbe\\.com\\.et/v2-([a-zA-Z0-9]+)$";
        Pattern pattern = Pattern.compile(patternTobeMatched);
        Matcher matcher = pattern.matcher(rawUri);

        if (!matcher.matches()) {
            return null;
        }

        String uniqueIdOnLink = matcher.group(1); //

        String requestTobeMadeLink = cbeJasonPayloadEndPoint + uniqueIdOnLink;


//        ResponseEntity<Void> preflightCheck= restClient.get()
//                .uri(requestTobeMadeLink)
//                .accept(MediaType.APPLICATION_JSON)// Tells server we want JSON
//                .header("x-app-version", x_app_version)
//                .header("x-app-id", x_app_id)
//                .header("user-agent", user_agent
//                )
//                .header("Origin", Origin)
//                .header("Referer", Referer)
//                .retrieve()
//                .toBodilessEntity();
//
//        if(!preflightCheck.getStatusCode().equals(HttpStatus.OK)){
//            return  null;
//        }





            ResponseEntity<String> response = restClient.get()
                    .uri(requestTobeMadeLink)
                    .accept(MediaType.APPLICATION_JSON)// Tells server we want JSON
                    .header("x-app-version", x_app_version)
                    .header("x-app-id", x_app_id)
                    .header("user-agent", user_agent
                    )
                    .header("Origin", Origin)
                    .header("Referer", Referer)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        System.out.println("The third-party server crashed! Status: " + res.getStatusCode());
                        // You can throw your own custom exception here, or handle it logging it.
                        // (Note: If you don't throw an exception here, Spring will try to continue parsing the body)
                        throw new CBE5xxServerException(res.getStatusText());
                    })
                    .toEntity(String.class);



            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                return null;
            }








        try {

            CbePayload rawCbePayload = objectMapper.convertValue(response.getBody(), CbePayload.class);
            return  rawCbePayload.withv2Key(uniqueIdOnLink);
        }

        catch (Exception exception){
            log.warn(" the response from the recipt server was ok but there was error in mapping process");
            throw exception;
        }


    }


    @Async
    public void archiveParticipantsOfAnEndedCampaign(Campaign campaign){
        if(!campaign.getStatus().equals(CampaignStatus.ENDED)){
            log.info("tryied to remove a campaign that is not ove  yet {}", campaign.getId());
            return;

        }

        for(CampaignParticipant campaignParticipant : campaignParticipantsRepository.findCampaignParticipantsByCampaign(campaign)){
            archivedCampaignParticipantsRepository.save(ArchivedCampaignParticipants.archivedCampaignParticipantFromCampaignParticipant(campaignParticipant));
            campaignParticipantsRepository.delete(campaignParticipant);
        }


    }

    @Transactional
    public void archiveParticipantsOfAnEndedCampaign(UUID campaignId){
        Campaign campaign = campaignRepository.findById(campaignId).orElseThrow();
        if(!campaign.getStatus().equals(CampaignStatus.ENDED)){
            log.info("tryied to remove a campaign that is not ove  yet {}", campaign.getId());
            return;

        }

        for(CampaignParticipant campaignParticipant : campaignParticipantsRepository.findCampaignParticipantsByCampaign(campaign)){
            archivedCampaignParticipantsRepository.save(ArchivedCampaignParticipants.archivedCampaignParticipantFromCampaignParticipant(campaignParticipant));
            campaignParticipantsRepository.delete(campaignParticipant);
        }


    }



    public void archiveParticipantsOfAnEndedCampaign2(Campaign campaign){ // to be used from the cron scheduler is non async

        for(CampaignParticipant campaignParticipant : campaignParticipantsRepository.findCampaignParticipantsByCampaign(campaign)){
            archivedCampaignParticipantsRepository.save(ArchivedCampaignParticipants.archivedCampaignParticipantFromCampaignParticipant(campaignParticipant));
            campaignParticipantsRepository.delete(campaignParticipant);
        }

    }

    public String extractV2KeyFromCBeLink(String  url){

        String patternTobeMatched = "^https://mbreciept\\.cbe\\.com\\.et/v2-([a-zA-Z0-9]+)$";
        Pattern pattern = Pattern.compile(patternTobeMatched);
        Matcher matcher = pattern.matcher(url);

        if (!matcher.matches()) {
            return null;
        }

        String uniqueIdOnLink = matcher.group(1); //

        return  uniqueIdOnLink;

    }


    public  String generateRandomPassword(int length){



        List<CharacterRule> rules = Arrays.asList(
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1)
        );

        PasswordGenerator passwordGenerator = new PasswordGenerator(length, rules);


        return  passwordGenerator.generate().toString();


    }

    public String addCampaignParticipantWithGeneratedRandomLotteryNumber(CampaignParticipant campaignParticipants){

        Random randomGen = new Random();
        int randomNumber =  randomGen.nextInt(100000000, 1000000000 );
        String randomEdilCode = String.valueOf(randomNumber);

        String edil = randomEdilCode.substring(0,3) + "-" + randomEdilCode.substring(3,6) + "-" + randomEdilCode.substring(6,9);



        try {
            if (campaignParticipantsRepository.existsByEdilCode(edil)) {
                return addCampaignParticipantWithGeneratedRandomLotteryNumber(campaignParticipants, randomGen);
            }

            campaignParticipants.setEdilCode(randomEdilCode);
            campaignParticipantsRepository.save(campaignParticipants);

            return randomEdilCode;
        }

        catch (ConstraintViolationException constraintViolationException){

            return addCampaignParticipantWithGeneratedRandomLotteryNumber(campaignParticipants, randomGen);
        }


    }


    public String addCampaignParticipantWithGeneratedRandomLotteryNumber(CampaignParticipant campaignParticipants, Random randomGen){
        int randomNumber =  randomGen.nextInt(100000000, 1000000000 );
        String randomEdilCode = String.valueOf(randomNumber);

        String edil = randomEdilCode.substring(0,3) + "-" + randomEdilCode.substring(3,6) + "-" + randomEdilCode.substring(6,9);


        try {
            if (campaignParticipantsRepository.existsByEdilCode(edil)) {
                return addCampaignParticipantWithGeneratedRandomLotteryNumber(campaignParticipants, randomGen);
            }

            campaignParticipants.setEdilCode(randomEdilCode);
            campaignParticipantsRepository.save(campaignParticipants);

            return randomEdilCode;
        }

        catch (ConstraintViolationException constraintViolationException){

            return addCampaignParticipantWithGeneratedRandomLotteryNumber(campaignParticipants, randomGen);
        }

    }



}

