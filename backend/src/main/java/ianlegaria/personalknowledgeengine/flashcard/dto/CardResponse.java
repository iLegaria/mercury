package ianlegaria.personalknowledgeengine.flashcard.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CardResponse(
        UUID id,
        UUID deckId,
        String question,
        String answer,
        int cardIndex,
        OffsetDateTime nextReviewAt,
        int intervalDays,
        int repetitions,
        double easeFactor
) {}
