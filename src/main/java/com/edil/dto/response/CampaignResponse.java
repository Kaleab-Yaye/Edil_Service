package com.edil.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private UUID id;
    private String title;
    private BigDecimal ticketPrice;
    private Integer targetEntries;
    private Integer joinedUsers;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String creatorName;
    private String firstPrizeImageUrl;
}
