package ianlegaria.personalknowledgeengine.chunk.service;

import java.util.List;

public interface ChunkingService {
    List<String> chunkText(String text, int chunkSize, int overlap);
    int countTokens(String text);
}
