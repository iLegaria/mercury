package ianlegaria.personalknowledgeengine.flashcard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewCardRequest(
        @NotNull UUID userId,
        @Min(0) @Max(5) int quality
) {}
