package com.edil.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "archived_campaign_prizes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchivedCampaignPrize {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "prize_order", nullable = false)
    private Integer prizeOrder;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "archived_at", nullable = false, updatable = false)
    private LocalDateTime archivedAt;
}
