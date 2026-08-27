package com.edil.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrizeResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer prizeOrder;
    private String imageUrl;
}
