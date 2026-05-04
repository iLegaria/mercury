package ianlegaria.personalknowledgeengine.chunk.service;

import ianlegaria.personalknowledgeengine.chunk.dto.RAGResponse;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface RAGService {

    RAGResponse ask(UUID userId, String question, UUID collectionId);

    Flux<String> askStream(UUID userId, String question, UUID collectionId);
}
