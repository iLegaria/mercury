package ianlegaria.personalknowledgeengine.chunk;

import ianlegaria.personalknowledgeengine.AbstractIntegrationTest;
import ianlegaria.personalknowledgeengine.chunk.dto.ChunkSearchResult;
import ianlegaria.personalknowledgeengine.chunk.service.EmbeddingService;
import ianlegaria.personalknowledgeengine.chunk.service.SemanticSearchService;
import ianlegaria.personalknowledgeengine.user.dto.CreateUserRequest;
import ianlegaria.personalknowledgeengine.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SemanticSearchServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired SemanticSearchService semanticSearchService;
    @Autowired RedisTemplate<String, Object> redisTemplate;
    @Autowired UserService userService;
    @MockBean EmbeddingService embeddingService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        userId = userService.createUser(new CreateUserRequest(unique + "@test.com", "Test")).getId();

        // Clear Redis before each test
        var keys = redisTemplate.keys("search:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        when(embeddingService.generateQueryEmbedding(anyString()))
                .thenReturn(Collections.nCopies(1024, 0.1));
        when(embeddingService.embeddingToString(any()))
                .thenReturn("[" + "0.1,".repeat(1023) + "0.1]");
    }

    @Test
    void search_missesCache_thenHitsCache() {
        String query = "test query about RAG";

        // First call — cache miss, should hit DB + embedding service
        List<ChunkSearchResult> result1 = semanticSearchService.search(userId, query, 5, null);

        // Redis key must exist after first call
        String expectedKey = "search:" + userId + ":" + query.toLowerCase().trim() + ":5:all";
        assertThat(redisTemplate.hasKey(expectedKey)).isTrue();

        // Second call — cache hit, embedding service must NOT be called again
        List<ChunkSearchResult> result2 = semanticSearchService.search(userId, query, 5, null);

        verify(embeddingService, times(1)).generateQueryEmbedding(query);
        assertThat(result2).isEqualTo(result1);
    }
}
