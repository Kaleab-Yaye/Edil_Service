package com.edil.dto.request;

import com.edil.domain.Petition;
import com.edil.domain.enums.PetitionReason;
import jakarta.validation.constraints.NotNull;

public record CreatePetitionRequest(
        String paymentLink,
       @NotNull PetitionReason petitionReason,
       @NotNull  String statement

) {
}
