package ianlegaria.personalknowledgeengine.chunk.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ianlegaria.personalknowledgeengine.common.config.EmbeddingProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final WebClient cohereWebClient;
    private final MeterRegistry meterRegistry;
    private final EmbeddingProperties embeddingProperties;

    @Value("${cohere.embedding-model}")
    private String model;

    @CircuitBreaker(name = "cohereAPI", fallbackMethod = "embeddingFallback")
    public List<Double> generateEmbedding(String text) {
        log.info("Generating embedding for text ({} chars)", text.length());
        List<Double> embedding = fetchEmbeddings(List.of(text), "search_document").get(0);
        meterRegistry.counter("embeddings.generated", "type", "document").increment();
        log.info("Embedding generated: {} dimensions", embedding.size());
        return embedding;
    }

    @CircuitBreaker(name = "cohereAPI", fallbackMethod = "embeddingFallback")
    public List<Double> generateQueryEmbedding(String query) {
        log.info("Generating query embedding ({} chars)", query.length());
        List<Double> embedding = fetchEmbeddings(List.of(query), "search_query").get(0);
        meterRegistry.counter("embeddings.generated", "type", "query").increment();
        log.info("Query embedding generated: {} dimensions", embedding.size());
        return embedding;
    }

    @CircuitBreaker(name = "cohereAPI", fallbackMethod = "batchEmbeddingFallback")
    public List<List<Double>> generateDocumentEmbeddingsBatch(List<String> texts) {
        List<List<Double>> allEmbeddings = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += embeddingProperties.getBatchSize()) {
            List<String> batch = texts.subList(i, Math.min(i + embeddingProperties.getBatchSize(), texts.size()));
            allEmbeddings.addAll(fetchEmbeddings(batch, "search_document"));
        }
        meterRegistry.counter("embeddings.generated", "type", "document").increment(texts.size());
        log.info("Batch embeddings generated: {} texts in {} API call(s)",
                texts.size(), (int) Math.ceil((double) texts.size() / embeddingProperties.getBatchSize()));
        return allEmbeddings;
    }

    private List<List<Double>> fetchEmbeddings(List<String> texts, String inputType) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "texts", texts,
                "input_type", inputType,
                "embedding_types", List.of("float")
        );

        CohereEmbedResponse response = cohereWebClient.post()
                .uri("/embed")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(CohereEmbedResponse.class)
                .block();

        if (response == null || response.embeddings() == null || response.embeddings().floatEmbeddings() == null) {
            throw new RuntimeException("Empty embedding response from Cohere");
        }

        return response.embeddings().floatEmbeddings();
    }

    private List<Double> embeddingFallback(String text, Throwable t) {
        log.error("Circuit breaker open — Cohere API unavailable: {}", t.getMessage());
        throw new RuntimeException("AI service is temporarily unavailable. Please try again in a few moments.");
    }

    private List<List<Double>> batchEmbeddingFallback(List<String> texts, Throwable t) {
        log.error("Circuit breaker open — Cohere API unavailable: {}", t.getMessage());
        throw new RuntimeException("AI service is temporarily unavailable. Please try again in a few moments.");
    }

    public String embeddingToString(List<Double> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CohereEmbedResponse(EmbeddingsByType embeddings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingsByType(@JsonProperty("float") List<List<Double>> floatEmbeddings) {}
}
