package com.edil.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GetJoinedPlayerInfoWithEdilNumberRequest(
        @NotNull UUID campaignId,
        @NotBlank String edilNumber
        )
{

}
