package com.edil.domain;

import com.edil.domain.enums.CampaignStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Account creator;

    @Column(nullable = false)
    private String title;

    @Column(name = "about_campaign", columnDefinition = "TEXT")
    private String aboutCampaign;

    @Column(name = "ticket_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal ticketPrice;

    @Column(name = "target_entries", nullable = false)
    private Integer targetEntries;

    @Column(name = "joined_users", nullable = false)
    @Builder.Default
    private Integer joinedUsers = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.PENDING;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "target_reached_at")
    private LocalDateTime targetReachedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActiveCampaignPrize> activePrizes;

    @OneToMany(mappedBy = "campaign")
    private List<ArchivedCampaignPrize> archivedCampaignPrizes;

    @Column(name = "started_being_processed_at")
    private LocalDateTime startedBeingProcessedAt;

    @OneToMany(mappedBy = "campaign")
    private List<CampaignParticipant> campaignParticipant;

    @OneToOne(mappedBy = "campaign")
    private CampaignParticipantsPdf campaignParticipantsPdf;



    @Column(name = "has_pdf")
    private boolean hadPdf;


    @Version @Getter @Setter Integer version;

}
