package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrizeRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Integer prizeOrder;

    @NotBlank
    private String imageFileId;


}
