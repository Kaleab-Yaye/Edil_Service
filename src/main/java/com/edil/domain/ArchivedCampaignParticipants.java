package com.edil.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name ="archived_Campaign_participant")
public class ArchivedCampaignParticipants {

    public static  ArchivedCampaignParticipants archivedCampaignParticipantFromCampaignParticipant(CampaignParticipants campaignParticipants){

    ArchivedCampaignParticipants archivedCampaignParticipants = new ArchivedCampaignParticipants();
    archivedCampaignParticipants.setId(campaignParticipants.getId());
    archivedCampaignParticipants.setCampaign(campaignParticipants.getCampaign());
    archivedCampaignParticipants.setAccount(campaignParticipants.getAccount());
    archivedCampaignParticipants.setReceiptHash(campaignParticipants.getReceiptHash());

    return  archivedCampaignParticipants;
    }

    @Id
    @Setter
    @Getter
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "account_id" )
    @Getter @Setter
    private Account account;

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    @Getter
    @Setter
    private  Campaign campaign;

    @Column(name ="receipt_hash")
    @Getter
    @Setter
    private String receiptHash;

    @Column(name = "archived_at")
    @Getter
    @Setter
    @UpdateTimestamp
    private LocalDateTime archivedAt;


}
