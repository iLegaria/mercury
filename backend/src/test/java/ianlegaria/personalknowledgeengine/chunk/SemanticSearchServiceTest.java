package ianlegaria.personalknowledgeengine.chunk;

import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.chunk.dto.ChunkSearchResult;
import ianlegaria.personalknowledgeengine.chunk.repository.ChunkRepository;
import ianlegaria.personalknowledgeengine.chunk.service.EmbeddingService;
import ianlegaria.personalknowledgeengine.chunk.service.SemanticSearchServiceImpl;
import ianlegaria.personalknowledgeengine.common.config.SearchProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock ChunkRepository chunkRepository;
    @Mock EmbeddingService embeddingService;
    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOps;

    private final SearchProperties searchProperties = new SearchProperties();
    private SemanticSearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        searchService = new SemanticSearchServiceImpl(
                chunkRepository, embeddingService, redisTemplate, new ObjectMapper(), searchProperties);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void search_cacheMiss_callsEmbeddingAndStoresResult() {
        UUID userId = UUID.randomUUID();
        String query = "semantic search explanation";
        List<Double> vector = List.of(0.1, 0.2, 0.3);

        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingService.generateQueryEmbedding(query)).thenReturn(vector);
        when(embeddingService.embeddingToString(vector)).thenReturn("[0.1,0.2,0.3]");
        when(chunkRepository.findSimilarChunks(any(), anyString(), anyInt())).thenReturn(List.of());

        searchService.search(userId, query, 5, null);

        verify(embeddingService).generateQueryEmbedding(query);
        verify(valueOps).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void search_cacheHit_returnsFromCacheWithoutCallingEmbedding() {
        UUID userId = UUID.randomUUID();
        String query = "cached query about RAG";
        ChunkSearchResult cachedResult = ChunkSearchResult.builder()
                .chunkId(UUID.randomUUID()).documentId(UUID.randomUUID())
                .content("cached content").chunkIndex(0).similarity(0.9).build();
        List<ChunkSearchResult> cachedList = List.of(cachedResult);

        // return the actual list — ObjectMapper.convertValue handles List<ChunkSearchResult> → List<ChunkSearchResult>
        when(valueOps.get(anyString())).thenReturn(cachedList);

        List<ChunkSearchResult> result = searchService.search(userId, query, 5, null);

        verify(embeddingService, never()).generateQueryEmbedding(anyString());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSimilarity()).isEqualTo(0.9);
    }

    @Test
    void search_cacheKeyIncludesCollectionId() {
        UUID userId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        String query = "query text";
        List<Double> vector = List.of(0.5);

        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingService.generateQueryEmbedding(query)).thenReturn(vector);
        when(embeddingService.embeddingToString(vector)).thenReturn("[0.5]");
        when(chunkRepository.findSimilarChunksInCollection(any(), anyString(), anyInt(), any())).thenReturn(List.of());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        searchService.search(userId, query, 5, collectionId);

        verify(valueOps).get(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).contains(collectionId.toString());
    }

    @Test
    void search_withoutCollectionId_cacheKeyContainsAll() {
        UUID userId = UUID.randomUUID();
        String query = "another query";
        List<Double> vector = List.of(0.1);

        when(valueOps.get(anyString())).thenReturn(null);
        when(embeddingService.generateQueryEmbedding(query)).thenReturn(vector);
        when(embeddingService.embeddingToString(vector)).thenReturn("[0.1]");
        when(chunkRepository.findSimilarChunks(any(), anyString(), anyInt())).thenReturn(List.of());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        searchService.search(userId, query, 5, null);

        verify(valueOps).get(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).endsWith(":all");
    }

    @Test
    void evictUserSearchCache_deletesMatchingKeys() {
        UUID userId = UUID.randomUUID();
        Set<String> matchingKeys = Set.of(
                "search:" + userId + ":query1:5:all",
                "search:" + userId + ":query2:5:all");
        when(redisTemplate.keys("search:" + userId + ":*")).thenReturn(matchingKeys);

        searchService.evictUserSearchCache(userId);

        verify(redisTemplate).delete(matchingKeys);
    }
}
