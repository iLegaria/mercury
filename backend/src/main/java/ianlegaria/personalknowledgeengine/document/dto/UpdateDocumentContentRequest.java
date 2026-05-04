package ianlegaria.personalknowledgeengine.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateDocumentContentRequest(
        @NotNull UUID userId,
        @NotBlank String content
) {}
