package ianlegaria.personalknowledgeengine.chunk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateChunkRequest(@NotNull UUID userId, @NotBlank String content) {}
