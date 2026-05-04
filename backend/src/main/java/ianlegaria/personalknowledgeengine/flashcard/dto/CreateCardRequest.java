package ianlegaria.personalknowledgeengine.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCardRequest(
        @NotNull UUID deckId,
        @NotNull UUID userId,
        @NotBlank @Size(max = 2000) String question,
        @NotBlank @Size(max = 2000) String answer
) {}
