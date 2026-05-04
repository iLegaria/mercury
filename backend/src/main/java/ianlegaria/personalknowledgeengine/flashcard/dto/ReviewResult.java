package ianlegaria.personalknowledgeengine.flashcard.dto;

import java.time.OffsetDateTime;

public record ReviewResult(
        OffsetDateTime nextReviewAt,
        int intervalDays
) {}
