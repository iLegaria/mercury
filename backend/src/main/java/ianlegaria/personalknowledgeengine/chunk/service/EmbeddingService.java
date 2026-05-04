package ianlegaria.personalknowledgeengine.chunk.service;

import java.util.List;

public interface EmbeddingService {

    List<Double> generateEmbedding(String text);

    List<Double> generateQueryEmbedding(String query);

    List<List<Double>> generateDocumentEmbeddingsBatch(List<String> texts);

    String embeddingToString(List<Double> embedding);
}
