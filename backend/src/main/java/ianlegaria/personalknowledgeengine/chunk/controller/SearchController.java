package ianlegaria.personalknowledgeengine.chunk.controller;

import ianlegaria.personalknowledgeengine.chunk.dto.*;
import ianlegaria.personalknowledgeengine.chunk.service.RAGService;
import ianlegaria.personalknowledgeengine.chunk.service.SemanticSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Tag(name = "Search & RAG", description = "Semantic search and retrieval-augmented generation")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SemanticSearchService semanticSearchService;
    private final RAGService ragService;

    @Operation(summary = "Semantic search",
            description = """
                    Embeds the query with Cohere embed-multilingual-v3.0, then searches pgvector
                    for the most similar document chunks using cosine similarity.
                    Results are cached in Redis for 10 minutes (cache key = userId + query + limit).
                    Rate-limited to prevent abuse.
                    """)
    @PostMapping
    public ResponseEntity<List<ChunkSearchResult>> search(@Valid @RequestBody SearchRequest request) {
        int limit = request.getLimit() != null ? request.getLimit() : 5;
        List<ChunkSearchResult> results = semanticSearchService.search(
                request.getUserId(),
                request.getQuery(),
                limit,
                request.getCollectionId()
        );
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Ask a question (RAG)",
            description = """
                    Full retrieval-augmented generation pipeline:
                    1. Embeds the question and retrieves the top-k most relevant document chunks from pgvector.
                    2. Filters chunks below minimum similarity threshold.
                    3. Builds a context window from the retrieved chunks.
                    4. Generates a grounded answer using Cohere Command-R.
                    The Cohere API call is protected by a Resilience4j circuit breaker.
                    Send Accept: application/json for a synchronous response.
                    """)
    @PostMapping(value = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RAGResponse> ask(@Valid @RequestBody AskRequest request) {
        RAGResponse response = ragService.ask(
                request.getUserId(),
                request.getQuestion(),
                request.getCollectionId()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Ask a question with streaming response (RAG)",
            description = """
                    Same RAG pipeline as /ask, but streams the answer token by token via SSE.
                    Send Accept: text/event-stream to activate streaming on the same endpoint.
                    Emits three event types as JSON:
                    - {"type":"sources","chunks":[...]} — retrieved context chunks (first event)
                    - {"type":"token","text":"..."} — individual tokens as they are generated
                    - {"type":"done"} — signals stream completion
                    """)
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@Valid @RequestBody AskRequest request) {
        return ragService.askStream(
                request.getUserId(),
                request.getQuestion(),
                request.getCollectionId()
        );
    }
}
