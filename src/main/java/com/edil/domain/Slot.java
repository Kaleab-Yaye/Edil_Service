package com.edil.domain;


import jakarta.persistence.*;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "slots")
public class Slot {
    @Id

    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    private UUID id;



    @Column(name = "campaign_id")
    @Setter
    @Getter
    private UUID  campaignId;


    @Column(name = "user_email")
    @Setter
    @Getter
    private String userEmail;



}
