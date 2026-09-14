package com.edil.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "campaign_participants_pdf")
@Entity
@Getter
@Setter
public class CampaignParticipantsPdf {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @OneToOne
    @JoinColumn(name = "campaign_id")
    Campaign campaign;

    @Column(name = "pdf_size_in_bytes")
    Long pdfSizeInBytes;

    @Column (name = "pdf_name")
    String pdfName;

    @UpdateTimestamp
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

}
