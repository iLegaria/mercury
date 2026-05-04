package ianlegaria.personalknowledgeengine.chunk.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingServiceImpl implements ChunkingService {

    @Override
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        if (text.length() <= chunkSize) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            boolean isLastChunk = (end == text.length());

            if (!isLastChunk) {
                int mid = start + chunkSize / 2;
                int paraBreak = text.lastIndexOf("\n\n", end);
                int lineBreak  = text.lastIndexOf('\n', end);
                int wordBreak  = text.lastIndexOf(' ', end);

                if      (paraBreak >= mid) end = paraBreak;
                else if (lineBreak >= mid) end = lineBreak;
                else if (wordBreak >= mid) end = wordBreak;
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (isLastChunk) break;
            start = end - overlap;
        }

        return chunks;
    }

    @Override
    public int countTokens(String text) {
        return text.split("\\s+").length;
    }
}
