package com.edil.dto.response;

public record ResolvePetitionResponse(
        boolean resolved,
        String message
) {
}
