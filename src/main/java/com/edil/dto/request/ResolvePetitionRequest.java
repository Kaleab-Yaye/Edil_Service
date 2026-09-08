package com.edil.dto.request;

import com.edil.domain.enums.PetitionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.checkerframework.checker.units.qual.UnknownUnits;

import java.util.UUID;

public record ResolvePetitionRequest(

       @NotNull UUID petitionsId,
       @NotNull PetitionStatus status

)
{

}
