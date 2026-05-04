package ianlegaria.personalknowledgeengine.document.service;

import ianlegaria.personalknowledgeengine.chunk.entity.ChunkEntity;
import ianlegaria.personalknowledgeengine.document.dto.DocumentIngestionEvent;

import java.util.List;
import java.util.UUID;

public interface IngestionPersistenceService {

    List<ChunkEntity> saveChunksWithoutEmbeddings(DocumentIngestionEvent event, String rawText);

    void saveEmbeddingsAndComplete(DocumentIngestionEvent event, List<ChunkEntity> chunks, List<List<Double>> embeddings);

    void markFailed(UUID documentId);
}
