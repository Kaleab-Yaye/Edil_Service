package com.edil.domain;

import com.edil.domain.enums.PetitionReason;
import com.edil.domain.enums.PetitionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "petitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Petition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "petitioner_id", nullable = false)
    private UserProfile petitioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolver_admin_id")
    private AdminProfile resolverAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "payment_link", nullable = false, length = 500)
    private String paymentLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private PetitionReason reason;

    @Column(name = "statement", nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    @Builder.Default
    private PetitionStatus status = PetitionStatus.UNRESOLVED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
