package ianlegaria.personalknowledgeengine.flashcard.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetWhatsappReminderRequest(
        @NotNull UUID userId,
        @NotNull Boolean enabled
) {}
