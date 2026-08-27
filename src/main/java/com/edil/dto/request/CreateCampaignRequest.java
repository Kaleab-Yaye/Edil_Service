package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {
    @NotBlank
    private String title;

    private String aboutCampaign;

    @NotNull
    @Positive
    private BigDecimal ticketPrice;

    @NotNull
    @Positive
    private Integer targetEntries;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;

    @NotEmpty
    private List<CreatePrizeRequest> prizes;
}
