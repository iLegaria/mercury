package ianlegaria.personalknowledgeengine.snippet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSnippetRequest(
        @NotNull UUID userId,
        @NotBlank String content,
        String sourceUrl,
        String sourceTitle
) {}
