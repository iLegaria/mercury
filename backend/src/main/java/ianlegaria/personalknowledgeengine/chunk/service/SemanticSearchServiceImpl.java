package ianlegaria.personalknowledgeengine.chunk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.chunk.dto.ChunkSearchResult;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.common.config.SearchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchServiceImpl implements SemanticSearchService {

    private final ChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SearchProperties searchProperties;

    @Transactional(readOnly = true)
    public List<ChunkSearchResult> search(UUID userId, String query, int limit, UUID collectionId) {
        log.info("Semantic search for user {}: '{}' (collection: {})", userId, query, collectionId);

        String cacheKey = buildCacheKey(userId, query, limit, collectionId);
        List<ChunkSearchResult> cached = getFromCache(cacheKey);
        if (cached != null) {
            log.info("Cache hit for query: '{}'", query);
            return cached;
        }

        List<Double> queryEmbedding = embeddingService.generateQueryEmbedding(query);
        String queryVector = embeddingService.embeddingToString(queryEmbedding);

        List<Object[]> results = collectionId != null
                ? chunkRepository.findSimilarChunksInCollection(userId, queryVector, limit, collectionId)
                : chunkRepository.findSimilarChunks(userId, queryVector, limit);

        List<ChunkSearchResult> searchResults = new ArrayList<>();
        for (Object[] row : results) {
            ChunkSearchResult result = ChunkSearchResult.builder()
                    .chunkId((UUID) row[0])
                    .documentId((UUID) row[1])
                    .content((String) row[2])
                    .chunkIndex((int) row[3])
                    .similarity(((Number) row[7]).doubleValue())
                    .documentTitle(row[8] != null ? (String) row[8] : null)
                    .build();
            searchResults.add(result);
        }

        saveToCache(cacheKey, searchResults);

        log.info("Found {} results, top similarity: {}",
                searchResults.size(),
                searchResults.isEmpty() ? "N/A" : String.format("%.4f", searchResults.get(0).getSimilarity()));

        return searchResults;
    }

    private String buildCacheKey(UUID userId, String query, int limit, UUID collectionId) {
        String colPart = collectionId != null ? collectionId.toString() : "all";
        return String.format("search:%s:%s:%d:%s", userId, query.toLowerCase().trim(), limit, colPart);
    }

    private List<ChunkSearchResult> getFromCache(String key) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.convertValue(cached, new TypeReference<>() {});
            }
        } catch (Exception e) {
            log.warn("Cache read failed: {}", e.getMessage());
        }
        return null;
    }

    private void saveToCache(String key, List<ChunkSearchResult> results) {
        try {
            redisTemplate.opsForValue().set(key, results, Duration.ofMinutes(searchProperties.getCacheTtlMinutes()));
        } catch (Exception e) {
            log.warn("Cache write failed: {}", e.getMessage());
        }
    }

    public void evictUserSearchCache(UUID userId) {
        String pattern = "search:" + userId + ":*";
        try {
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} search cache entries for user {}", keys.size(), userId);
            }
        } catch (Exception e) {
            log.warn("Cache eviction failed for user {}: {}", userId, e.getMessage());
        }
    }
}
