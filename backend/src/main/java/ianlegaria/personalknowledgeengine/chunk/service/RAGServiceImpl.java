package ianlegaria.personalknowledgeengine.chunk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ianlegaria.personalknowledgeengine.chunk.dto.ChunkSearchResult;
import ianlegaria.personalknowledgeengine.chunk.dto.RAGResponse;
import ianlegaria.personalknowledgeengine.common.client.CohereClient;
import ianlegaria.personalknowledgeengine.common.config.RagProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements RAGService {

    private final SemanticSearchService semanticSearchService;
    private final CohereClient cohereClient;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final RagProperties ragProperties;

    private static final String SYSTEM_PROMPT = """
            You are a personal knowledge assistant. Answer the user's question based ONLY on the provided context.

            Rules:
            - Only use information from the context below
            - If the context doesn't contain enough information, say so honestly
            - Reference which source you used when possible
            - Be concise and direct
            - Answer in the same language the user asks in
            """;

    public RAGResponse ask(UUID userId, String question, UUID collectionId) {
        log.info("RAG query from user {}: '{}' (collection: {})", userId, question, collectionId);
        meterRegistry.counter("rag.queries").increment();

        List<ChunkSearchResult> chunks = semanticSearchService.search(userId, question, ragProperties.getMaxChunks(), collectionId);

        List<ChunkSearchResult> relevantChunks = chunks.stream()
                .filter(c -> c.getSimilarity() >= ragProperties.getMinSimilarity())
                .toList();

        if (relevantChunks.isEmpty()) {
            log.info("No relevant chunks found for query");
            return RAGResponse.builder()
                    .answer("I couldn't find any relevant information in your documents about that topic. Try uploading more documents or rephrasing your question.")
                    .chunks(List.of())
                    .build();
        }

        String context = buildContext(relevantChunks);
        String answer = generateAnswer(question, context);

        return RAGResponse.builder()
                .answer(answer)
                .chunks(relevantChunks)
                .build();
    }

    private String buildContext(List<ChunkSearchResult> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkSearchResult chunk = chunks.get(i);
            sb.append(String.format("[Source %d (%.0f%% relevant)]\n%s\n\n",
                    i + 1,
                    chunk.getSimilarity() * 100,
                    chunk.getContent()));
        }
        return sb.toString();
    }

    private String generateAnswer(String question, String context) {
        return cohereClient.chat(SYSTEM_PROMPT, buildUserMessage(question, context));
    }

    private String buildUserMessage(String question, String context) {
        return String.format("""
                Context from my documents:
                %s

                My question: %s
                """, context, question);
    }

    public Flux<String> askStream(UUID userId, String question, UUID collectionId) {
        log.info("RAG stream query from user {}: '{}'", userId, question);
        meterRegistry.counter("rag.queries").increment();

        List<ChunkSearchResult> chunks = semanticSearchService.search(userId, question, ragProperties.getMaxChunks(), collectionId);
        List<ChunkSearchResult> relevantChunks = chunks.stream()
                .filter(c -> c.getSimilarity() >= ragProperties.getMinSimilarity())
                .toList();

        if (relevantChunks.isEmpty()) {
            return Flux.just(
                    toEvent("token", Map.of("text", "I couldn't find any relevant information in your documents about that topic. Try uploading more documents or rephrasing your question.")),
                    "{\"type\":\"done\"}"
            );
        }

        String sourcesJson = toEvent("sources", Map.of("chunks", relevantChunks));
        String context = buildContext(relevantChunks);

        Flux<String> tokenFlux = cohereClient.chatStream(SYSTEM_PROMPT, buildUserMessage(question, context))
                .map(token -> toEvent("token", Map.of("text", token)))
                .concatWith(Flux.just("{\"type\":\"done\"}"));

        return Flux.concat(Flux.just(sourcesJson), tokenFlux);
    }

    private String toEvent(String type, Map<String, Object> fields) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", type);
            event.putAll(fields);
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return "{\"type\":\"" + type + "\"}";
        }
    }
}
