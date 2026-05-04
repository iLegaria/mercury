package ianlegaria.personalknowledgeengine.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateDocumentTitleRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 500) String title
) {}
