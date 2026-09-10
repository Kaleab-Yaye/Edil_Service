package com.edil.dto.response;

import com.edil.domain.enums.PetitionReason;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record GetPetitionsBeingHandledByMeResponse
        (
                UUID id,
                UUID petitionerId,
                UUID campaignId,
                String paymentLink,
                PetitionReason petitionReason,
                LocalDateTime createdAt
        ){
}
