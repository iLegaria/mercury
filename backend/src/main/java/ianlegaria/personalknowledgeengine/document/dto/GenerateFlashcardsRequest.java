package ianlegaria.personalknowledgeengine.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateFlashcardsRequest(
        @NotNull(message = "User ID is required") UUID userId,
        String mode,  // "EXTRACT" | "GENERATE"
        @Min(3) @Max(50) Integer numCards
) {
    public String resolvedMode() { return mode != null ? mode.toUpperCase() : "GENERATE"; }
    public int resolvedNumCards() { return numCards != null ? numCards : 10; }
}
