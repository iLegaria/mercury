package ianlegaria.personalknowledgeengine.snippet.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SnippetResponse(
        UUID id,
        UUID userId,
        String content,
        String sourceUrl,
        String sourceTitle,
        OffsetDateTime createdAt
) {}
