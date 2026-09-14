package com.edil.util;


import com.edil.domain.*;
import com.edil.domain.enums.CampaignStatus;
import com.edil.exception.CampaignNotFoundException;
import com.edil.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Logger;
import org.hibernate.boot.jaxb.internal.stax.LocalSchemaLocator;
import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveCampaignAndGeneratePdfUtil {



    public void handleArchivalAndGeneratePdfUtil(

            ArchivedCampaignParticipantsRepository archivedCampaignParticipantsRepository,
            ArchivedCampaignPrizeRepository archivedCampaignPrizeRepository,
            CampaignParticipantPdfRepository campaignParticipantPdfRepository,
            CampaignRepository campaignRepository,
            CampaignParticipantsRepository campaignParticipantsRepository,
            UUID campaignId,
            AtomicInteger openSlot

    ) {

        try {

            final String pdfSaveLocation = "./pdf_store"; // needs to go to env

            Campaign campaign = campaignRepository.getCampaignsById(campaignId).orElseThrow(() -> new CampaignNotFoundException(campaignId.toString()));
            List<CampaignParticipant> campaignParticipants = campaign.getCampaignParticipant();
            List<ActiveCampaignPrize> activePrizes = campaign.getActivePrizes();

            // start of prize archival

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

            // end of archival

            if (campaignParticipants.isEmpty()) {
                return;
            }

            String pdfName = campaign.getTitle() + "-" + campaign.getStartDate().toString();
            String pdfSavePath = pdfSaveLocation + "/" + pdfName;

            String titleValue = "Campaign---" + campaign.getTitle() + " participants";

            try {
                Document document = new Document(PageSize.A4, 36, 36, 36, 36);
                PdfWriter.getInstance(document, new FileOutputStream(pdfSavePath));
                document.open();


                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
                Paragraph title = new Paragraph("EDIL CAMPAIGN: " + titleValue, titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20f); //
                document.add(title);

                PdfPTable table = new PdfPTable(3);
                table.setWidthPercentage(100); // Stretch across the whole page
                table.setWidths(new float[]{1.5f, 2.5f, 2f});

                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
                table.addCell(createStyledCell("order", headerFont));
                table.addCell(createStyledCell("edilCode", headerFont));
                table.addCell(createStyledCell("fullName", headerFont));
                table.addCell(createStyledCell("phoneNumber", headerFont));
                table.addCell(createStyledCell("refundBankAccount", headerFont));
                table.addCell(createStyledCell("address", headerFont));

                table.setHeaderRows(1);


                Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);

                // here each entry will be removed from the campaign participant and get archived at the same time it is getting added
                int order = 1;
                for (CampaignParticipant campaignParticipant : campaignParticipants) {
                    UserProfile userProfile = campaignParticipant.getAccount().getUserProfile();
                    table.addCell(createStyledCell(String.valueOf(order), rowFont));
                    table.addCell(createStyledCell(campaignParticipant.getEdilCode(), rowFont));
                    table.addCell(createStyledCell(userProfile.getFullName(), rowFont));
                    table.addCell(createStyledCell(userProfile.getPhoneNumber(), rowFont));
                    table.addCell(createStyledCell(userProfile.getRefundBankAccount(), rowFont));
                    table.addCell(createStyledCell(userProfile.getAddress(), rowFont));

                    // start of archival
                    archivedCampaignParticipantsRepository.save(campaignParticipant.archivedCampaignParticipant());
                    campaignParticipantsRepository.delete(campaignParticipant);
                }

                campaign.setStatus(CampaignStatus.ENDED_BY_CREATOR_PROCESSED);
                campaign.setHasPdf(true);
                campaignRepository.save(campaign);

                document.add(table);
                document.close();

                Long pdfSizeInBytes = Files.size(Paths.get(pdfSavePath));

                // create the pdf table lol

                CampaignParticipantsPdf campaignParticipantsPdf = new CampaignParticipantsPdf();
                campaignParticipantsPdf.setCampaign(campaign);
                campaignParticipantsPdf.setPdfName(pdfName);
                campaignParticipantsPdf.setPdfSizeInBytes(pdfSizeInBytes);
                campaignParticipantsPdf.setGeneratedAt(LocalDateTime.now());
                campaignParticipantPdfRepository.save(campaignParticipantsPdf);

                // signal an open slot for the rest
                openSlot.incrementAndGet();


                log.info("the campaign wit the id {} is archived and pdf with the size of {} is generated for it", campaignId, pdfSizeInBytes);


            } catch (Exception exception
            ) {
                openSlot.incrementAndGet();
                log.warn("exception with the follwoing messege was invoked while trying to generate pdf {} and the stack trace is {}",exception.getMessage(), exception.getStackTrace());
                throw new RuntimeException("pdf processing failed");


            }

        }

        catch (Exception exception){

            openSlot.incrementAndGet();
            log.warn("exception with the follwoing messege was invoked while trying to generate pdf and archive campaigni {} and the stack trace is {}",exception.getMessage(), exception.getStackTrace());
            throw new RuntimeException("thread level trasaction of archival and pdg generation failed");
        }


    }

    private PdfPCell createStyledCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));


        cell.setBorder(Rectangle.NO_BORDER);


        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(Color.LIGHT_GRAY);


        cell.setPaddingBottom(8f);
        cell.setPaddingTop(8f);

        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        return cell;
    }


}
