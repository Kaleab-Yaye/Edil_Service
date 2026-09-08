package com.edil.dto.response;

import com.edil.domain.Petition;
import com.edil.domain.enums.PetitionReason;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;


@Builder
public record GetUnresolvedPetitionsResponse(

        UUID id,
        UUID petitionerId,
        UUID campaignId,
        String paymentLink,
        PetitionReason petitionReason,
        LocalDateTime createdAt


) {
}
