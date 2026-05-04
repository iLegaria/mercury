package ianlegaria.personalknowledgeengine.flashcard.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeckResponse(
        UUID id,
        UUID userId,
        String name,
        String description,
        long cardCount,
        long dueCount,
        OffsetDateTime createdAt,
        boolean whatsappReminderEnabled
) {}
