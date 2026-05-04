package ianlegaria.personalknowledgeengine.common.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CohereClient {

    private final WebClient cohereWebClient;
    private final ObjectMapper objectMapper;

    @Value("${cohere.chat-model}")
    private String chatModel;

    @CircuitBreaker(name = "cohereAPI", fallbackMethod = "chatFallback")
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", chatModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        CohereGenerateResponse response = cohereWebClient.post()
                .uri("/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(CohereGenerateResponse.class)
                .block();

        if (response == null || response.message() == null
                || response.message().content() == null
                || response.message().content().isEmpty()) {
            throw new RuntimeException("Empty response from Cohere");
        }

        String text = response.message().content().get(0).text();
        log.debug("Cohere chat response: {} chars", text.length());
        return text;
    }

    private String chatFallback(String systemPrompt, String userMessage, Throwable t) {
        log.error("Circuit breaker open — Cohere API unavailable: {}", t.getMessage());
        throw new RuntimeException("AI service is temporarily unavailable. Please try again in a few moments.");
    }

    public Flux<String> chatStream(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", chatModel,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        return cohereWebClient.post()
                .uri("/chat")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(sse -> extractStreamToken(sse.data()));
    }

    private String extractStreamToken(String data) {
        if (data == null) return null;
        try {
            JsonNode node = objectMapper.readTree(data);
            if (!"content-delta".equals(node.path("type").asText())) return null;
            String text = node.path("delta").path("message").path("content").path("text").asText(null);
            return (text != null && !text.isEmpty()) ? text : null;
        } catch (Exception e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CohereGenerateResponse(CohereMessage message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CohereMessage(List<CohereContent> content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CohereContent(String type, String text) {}
}
