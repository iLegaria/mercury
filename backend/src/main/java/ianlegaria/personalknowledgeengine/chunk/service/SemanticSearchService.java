package ianlegaria.personalknowledgeengine.chunk.service;

import ianlegaria.personalknowledgeengine.chunk.dto.ChunkSearchResult;

import java.util.List;
import java.util.UUID;

public interface SemanticSearchService {

    List<ChunkSearchResult> search(UUID userId, String query, int limit, UUID collectionId);

    void evictUserSearchCache(UUID userId);
}
