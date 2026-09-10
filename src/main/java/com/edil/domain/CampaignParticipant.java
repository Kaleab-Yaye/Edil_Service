package com.edil.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="campaign_participants")
public class CampaignParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Column(name = "added_at")
    @Getter
    @Setter
    @UpdateTimestamp
    private LocalDateTime addedAt;

    @ManyToOne
    @JoinColumn(name = "adder_admin_id")
    @Getter
    @Setter
    private AdminProfile adminProfile;

    @Column(name = "edil_code")
    @Getter
    @Setter
    private String edilCode;


}
