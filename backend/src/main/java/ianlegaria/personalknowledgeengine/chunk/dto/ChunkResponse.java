package ianlegaria.personalknowledgeengine.chunk.dto;

import java.util.UUID;

public record ChunkResponse(UUID chunkId, int chunkIndex, String content, Integer tokenCount) {}
