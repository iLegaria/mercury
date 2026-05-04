package ianlegaria.personalknowledgeengine.chunk.service;

import ianlegaria.personalknowledgeengine.chunk.dto.ChunkResponse;

import java.util.List;
import java.util.UUID;

public interface ChunkService {

    List<ChunkResponse> getChunksForDocument(UUID documentId, UUID userId);

    ChunkResponse updateChunk(UUID chunkId, UUID userId, String newContent);
}
