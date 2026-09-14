package com.edil.service;

import com.edil.config.util.CampaignToSlotNumberAndSlotKeyToCampaignBuilder;
import com.edil.config.util.StoreCampaignToSlotHashMap;
import com.edil.domain.Account;
import com.edil.domain.ActiveCampaignPrize;
import com.edil.domain.ArchivedCampaignPrize;
import com.edil.domain.Campaign;
import com.edil.domain.enums.CampaignStatus;
import com.edil.dto.request.CreateCampaignRequest;
import com.edil.dto.request.GetCampaignPaymentInfoRequest;
import com.edil.dto.response.CampaignDetailResponse;
import com.edil.dto.response.CampaignResponse;
import com.edil.dto.response.GetCampaignPaymentInfoResponse;
import com.edil.dto.response.PrizeResponse;
import com.edil.exception.CampaignNotFoundException;
import com.edil.repository.AccountRepository;
import com.edil.repository.ActiveCampaignPrizeRepository;
import com.edil.repository.ArchivedCampaignPrizeRepository;
import com.edil.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.edil.domain.enums.OnboardingStatus;
import com.edil.exception.AccountNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final ActiveCampaignPrizeRepository activeCampaignPrizeRepository;
    private final ArchivedCampaignPrizeRepository archivedCampaignPrizeRepository;
    private final AccountRepository accountRepository;
    private final UploadService uploadService;
    private final CampaignServiceUtil campaignServiceUtil;
    private final CampaignParticipantServiceUtil campaignParticipantServiceUtil;
    private final StoreCampaignToSlotHashMap campaignToSlotHashMap;

    @Transactional
    public CampaignResponse createCampaign(String creatorEmail, CreateCampaignRequest request) {
        log.info("createCampaign service hit for email: {}", creatorEmail);
        Account creator = accountRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new AccountNotFoundException("Creator account not found"));

        if (creator.getCreatorProfile() == null || creator.getCreatorProfile().getOnboardingStatus() != OnboardingStatus.ONBOARDED) {
            throw new IllegalStateException("Creator must be ONBOARDED before creating campaigns");
        }

        List<CampaignStatus> activeStatuses = List.of(CampaignStatus.PENDING, CampaignStatus.APPROVED);
        if (campaignRepository.existsByCreatorIdAndStatusIn(creator.getId(), activeStatuses)) {
            throw new IllegalStateException("Creator already has an active or pending campaign. Complete or wait for the existing campaign to end before creating a new one.");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        Campaign campaign = Campaign.builder()
                .creator(creator)
                .title(request.getTitle())
                .aboutCampaign(request.getAboutCampaign())
                .ticketPrice(request.getTicketPrice())
                .targetEntries(request.getTargetEntries())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(CampaignStatus.PENDING)
                .build();

        List<ActiveCampaignPrize> activePrizes = request.getPrizes().stream().map(p -> {
            if (!uploadService.isUploadConfirmed(p.getImageFileId())) {
                throw new IllegalStateException("Image upload not confirmed for file: " + p.getImageFileId());
            }

            ActiveCampaignPrize prize = ActiveCampaignPrize.builder()
                    .campaign(campaign)
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .prizeOrder(p.getPrizeOrder())
                    .imageUrl("http://localhost:8081/static/" + p.getImageFileId())
                    .build();

            uploadService.invalidateTicket(p.getImageFileId());
            return prize;
        }).collect(Collectors.toList());

        campaign.setActivePrizes(activePrizes);
        Campaign savedCampaign = campaignRepository.save(campaign);
        return mapToCampaignResponse(savedCampaign);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> getPublicCampaigns(Pageable pageable) {
        return campaignRepository.findByStatus(CampaignStatus.APPROVED, pageable)
                .map(this::mapToCampaignResponse);
    }


    public Page<CampaignResponse> getPublicCampaignsEnded(Pageable pageable){

        return campaignRepository.findByStatus(CampaignStatus.ENDED, pageable)
                .map(this::mapToEndedCampaignResponse);


    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getPendingCampaigns() {
        return campaignRepository.findByStatus(CampaignStatus.PENDING, Pageable.unpaged())
                .getContent().stream()
                .map(this::mapToCampaignResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getCampaignsByCreatorId(UUID creatorId) {
        return campaignRepository.findByCreatorId(creatorId).stream()
                .map(this::mapToCampaignResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CampaignDetailResponse getCampaignDetail(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        List<PrizeResponse> prizeResponses;
        if (campaign.getStatus() == CampaignStatus.ENDED) {
            prizeResponses = archivedCampaignPrizeRepository.findByCampaignIdOrderByPrizeOrderAsc(campaign.getId())
                    .stream().map(p -> PrizeResponse.builder()
                            .id(p.getId())
                            .title(p.getTitle())
                            .description(p.getDescription())
                            .prizeOrder(p.getPrizeOrder())
                            .imageUrl(p.getImageUrl())
                            .build()).collect(Collectors.toList());
        } else {
            prizeResponses = activeCampaignPrizeRepository.findByCampaignIdOrderByPrizeOrderAsc(campaign.getId())
                    .stream().map(p -> PrizeResponse.builder()
                            .id(p.getId())
                            .title(p.getTitle())
                            .description(p.getDescription())
                            .prizeOrder(p.getPrizeOrder())
                            .imageUrl(p.getImageUrl())
                            .build()).collect(Collectors.toList());
        }

        String creatorName = null;
        String creatorChannelLink = null;
        String creatorAbout = null;
        if (campaign.getCreator() != null && campaign.getCreator().getCreatorProfile() != null) {
            creatorName = campaign.getCreator().getCreatorProfile().getFullName();
            creatorChannelLink = campaign.getCreator().getCreatorProfile().getChannelLink();
            creatorAbout = campaign.getCreator().getCreatorProfile().getAboutSection();
        }



        return CampaignDetailResponse.builder()
                .id(campaign.getId())
                .title(campaign.getTitle())
                .aboutCampaign(campaign.getAboutCampaign())
                .ticketPrice(campaign.getTicketPrice())
                .targetEntries(campaign.getTargetEntries())
                .joinedUsers(campaign.getJoinedUsers())
                .status(campaign.getStatus().name())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .creatorName(creatorName)
                .creatorChannelLink(creatorChannelLink)
                .creatorAbout(creatorAbout)
                .prizes(prizeResponses)
                .availableSlots(StoreCampaignToSlotHashMap.campaignToSlotStore.get(campaign.getId()) != null 
                        ? StoreCampaignToSlotHashMap.campaignToSlotStore.get(campaign.getId()).intValue() 
                        : Math.max(0, campaign.getTargetEntries() - campaign.getJoinedUsers()))
                .build();
    }

    @Transactional
    public void approveCampaign(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new AccountNotFoundException("Campaign not found"));
        campaign.setStatus(CampaignStatus.APPROVED);

        campaignRepository.save(campaign);
        // async methode
        campaignServiceUtil.addCampaignToCampaignToAvailableSlotMap(campaign);
    }

    @Transactional
    public void rejectCampaign(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new AccountNotFoundException("Campaign not found"));
        campaign.setStatus(CampaignStatus.REJECTED);
        campaignRepository.save(campaign);
    }

    @Transactional
    public void archiveExpiredCampaigns() {
        List<Campaign> expiredCampaigns = campaignRepository.findByStatusAndEndDateLessThanEqual(CampaignStatus.APPROVED, LocalDateTime.now());
        for (Campaign campaign : expiredCampaigns) {
            campaign.setStatus(CampaignStatus.ENDED);

            List<ActiveCampaignPrize> activePrizes = activeCampaignPrizeRepository.findByCampaignIdOrderByPrizeOrderAsc(campaign.getId());
            List<ArchivedCampaignPrize> archivedPrizes = new ArrayList<>();

            for (ActiveCampaignPrize activePrize : activePrizes) {
                ArchivedCampaignPrize archivedPrize = ArchivedCampaignPrize.builder()
                        .campaign(campaign)
                        .title(activePrize.getTitle())
                        .description(activePrize.getDescription())
                        .prizeOrder(activePrize.getPrizeOrder())
                        .imageUrl(activePrize.getImageUrl())
                        .build();
                archivedPrizes.add(archivedPrize);
            }

            campaignParticipantServiceUtil.archiveParticipantsOfAnEndedCampaign2(campaign);




            archivedCampaignPrizeRepository.saveAll(archivedPrizes);
            activeCampaignPrizeRepository.deleteByCampaignId(campaign.getId());
        }
        campaignRepository.saveAll(expiredCampaigns);
    }



    private CampaignResponse mapToCampaignResponse(Campaign campaign) {
        String firstPrizeImageUrl = null;
        if (campaign.getActivePrizes() != null && !campaign.getActivePrizes().isEmpty()) {
            firstPrizeImageUrl = campaign.getActivePrizes().get(0).getImageUrl();
        } else {
            List<ActiveCampaignPrize> activePrizes = activeCampaignPrizeRepository.findByCampaignIdOrderByPrizeOrderAsc(campaign.getId());
            if (!activePrizes.isEmpty()) firstPrizeImageUrl = activePrizes.get(0).getImageUrl();
        }

        String creatorName = null;
        if (campaign.getCreator() != null && campaign.getCreator().getCreatorProfile() != null) {

            creatorName = campaign.getCreator().getCreatorProfile().getFullName();
            log.info("logging the creator  name and it is {}", creatorName);
        }

        return CampaignResponse.builder()
                .id(campaign.getId())
                .title(campaign.getTitle())
                .ticketPrice(campaign.getTicketPrice())
                .targetEntries(campaign.getTargetEntries())
                .joinedUsers(campaign.getJoinedUsers())
                .status(campaign.getStatus().name())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .creatorName(creatorName)
                .firstPrizeImageUrl(firstPrizeImageUrl)
                .build();
    }


    // am let the AI ts shit it is broing asf

    private CampaignResponse mapToEndedCampaignResponse(Campaign campaign) {
        String firstPrizeImageUrl = null;
        if (campaign.getArchivedCampaignPrizes() != null && !campaign.getArchivedCampaignPrizes().isEmpty()) {
            firstPrizeImageUrl = campaign.getArchivedCampaignPrizes().get(0).getImageUrl();
        } else {
            List<ArchivedCampaignPrize> archivedPrizes = archivedCampaignPrizeRepository.findByCampaignIdOrderByPrizeOrderAsc(campaign.getId());
            if (!archivedPrizes.isEmpty()) firstPrizeImageUrl = archivedPrizes.get(0).getImageUrl();
        }

        String creatorName = null;
        if (campaign.getCreator() != null && campaign.getCreator().getCreatorProfile() != null) {

            creatorName = campaign.getCreator().getCreatorProfile().getFullName();
            log.info("logging the creator  name and it is {}", creatorName);
        }

        return CampaignResponse.builder()
                .id(campaign.getId())
                .title(campaign.getTitle())
                .ticketPrice(campaign.getTicketPrice())
                .targetEntries(campaign.getTargetEntries())
                .joinedUsers(campaign.getJoinedUsers())
                .status(campaign.getStatus().name())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .creatorName(creatorName)
                .firstPrizeImageUrl(firstPrizeImageUrl)
                .build();
    }

    // i think the logic that desides if the user can decide should be here or what?



    public Optional<Campaign> getCampaignById(UUID campaignId) {
        return campaignRepository.getCampaignsById(campaignId);
    }


    @Transactional
    public boolean updateUserCount(UUID campaignId) {
        Campaign campaign = campaignRepository.getCampaignsById(campaignId).orElseThrow(()->new CampaignNotFoundException(campaignId.toString()));

        log.info("this is to be printed first jsut to check ");

        log.info("the number of already joinged users is {}", campaign.getJoinedUsers());

        campaign.setJoinedUsers(campaign.getJoinedUsers() + 1);

        if (campaign.getJoinedUsers() >= campaign.getTargetEntries()) {
            campaign.setTargetReachedAt(LocalDateTime.now());
            campaign.setStatus(CampaignStatus.ENDED);
            campaignRepository.save(campaign);

            // async methode
            archivePrizesForAnEndedCampaign(campaign);


            return true;


        }

        campaignRepository.save(campaign);
        return false;


    }


    @Transactional
    public boolean updateUserCount(Campaign campaign) {


        log.info("this is to be printed first jsut to check ");

        log.info("the number of already joinged users is {}", campaign.getJoinedUsers());

        campaign.setJoinedUsers(campaign.getJoinedUsers() + 1);

        if (campaign.getJoinedUsers() >= campaign.getTargetEntries()) {
            campaign.setTargetReachedAt(LocalDateTime.now());
            campaign.setStatus(CampaignStatus.ENDED);
            campaignRepository.save(campaign);

            // async methode
            archivePrizesForAnEndedCampaign(campaign);


            return true;


        }

        campaignRepository.save(campaign);
        return false;


    }



    @Async
    public void archivePrizesForAnEndedCampaign(Campaign campaign) {


        List<ActiveCampaignPrize> activePrizes = activeCampaignPrizeRepository.findByCampaignIdOrderByPrizeOrderAsc(campaign.getId());
        List<ArchivedCampaignPrize> archivedPrizes = new ArrayList<>();

        for (ActiveCampaignPrize activePrize : activePrizes) {
            ArchivedCampaignPrize archivedPrize = ArchivedCampaignPrize.builder()
                    .campaign(campaign)
                    .title(activePrize.getTitle())
                    .description(activePrize.getDescription())
                    .prizeOrder(activePrize.getPrizeOrder())
                    .imageUrl(activePrize.getImageUrl())
                    .build();
            archivedPrizes.add(archivedPrize);
        }

        archivedCampaignPrizeRepository.saveAll(archivedPrizes);
        activeCampaignPrizeRepository.deleteByCampaignId(campaign.getId());
    }





//    public boolean updateUserCount(Campaign campaign) {
//
//        campaign.setJoinedUsers(campaign.getJoinedUsers() + 1); // well even the limit is hit some how adding one user won't hurt that much
//
//        if (campaign.getJoinedUsers() >= campaign.getTargetEntries()) {
//            campaign.setTargetReachedAt(LocalDateTime.now());
//            campaign.setStatus(CampaignStatus.ENDED);
//            campaignRepository.save(campaign);
//
//            // async methode
//            archivePrizesForAnEndedCampaign(campaign);
//
//
//            return true;
//
//
//        }
//
//        campaignRepository.save(campaign);
//        return false;
//
//
//    }

   public List<Campaign> getAllRunningCampaigns(){

        return  campaignRepository.findByStatus(CampaignStatus.APPROVED);

   }

   public List<Campaign> getPageableByStatus(CampaignStatus campaignStatus, Pageable pageable){

        return campaignRepository.findAllByStatus(campaignStatus, pageable);

   }

   public List<Campaign> getPageableByStatusAndTime(CampaignStatus campaignStatus , LocalDateTime localDateTime , Pageable pageable){
       return  campaignRepository.findByStatusAndStartedBeingProcessedAtLessThanEqual(campaignStatus, localDateTime, pageable);
   };




   public ResponseEntity<GetCampaignPaymentInfoResponse> getCampaignPaymentInfo(UUID campaignId){
        Campaign campaign  = getCampaignById(campaignId).orElseThrow(()->new CampaignNotFoundException(campaignId.toString()));
        if(!campaign.getStatus().equals(CampaignStatus.APPROVED)){
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
       }



       return ResponseEntity.status(HttpStatus.OK).body(
                new GetCampaignPaymentInfoResponse(campaign.getTicketPrice(), campaign.getCreator().getCreatorProfile().getPayoutBankAccount(),
                        campaign.getCreator().getCreatorProfile().getFullName()
                        )
        );
   }


}
