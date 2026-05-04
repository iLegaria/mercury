package ianlegaria.personalknowledgeengine.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartDocumentQuizRequest(
        @NotNull(message = "User ID is required") UUID userId,
        String generationMode,  // "EXTRACT" | "GENERATE"
        @Min(3) @Max(15) Integer numQuestions
) {
    public String resolvedGenerationMode() { return generationMode != null ? generationMode.toUpperCase() : "GENERATE"; }
    public int resolvedNumQuestions() { return numQuestions != null ? numQuestions : 5; }
}
