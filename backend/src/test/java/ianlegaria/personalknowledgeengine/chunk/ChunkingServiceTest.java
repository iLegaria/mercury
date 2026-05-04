package ianlegaria.personalknowledgeengine.chunk;

import ianlegaria.personalknowledgeengine.chunk.service.ChunkingServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private final ChunkingServiceImpl chunkingService = new ChunkingServiceImpl();

    @Test
    void chunkText_shortText_returnsSingleChunk() {
        String text = "a".repeat(1000);

        List<String> chunks = chunkingService.chunkText(text, 2000, 200);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    @Test
    void chunkText_longText_chunksWithOverlap() {
        // 2500 chars of 'x' (no whitespace) → hard cut at 2000, overlap 200
        String text = "x".repeat(2500);

        List<String> chunks = chunkingService.chunkText(text, 2000, 200);

        assertThat(chunks).hasSize(2);

        String overlapFromFirst = chunks.get(0).substring(chunks.get(0).length() - 200);
        String overlapFromSecond = chunks.get(1).substring(0, 200);
        assertThat(overlapFromFirst).isEqualTo(overlapFromSecond);
    }

    @Test
    void chunkText_paragraphText_splitsAtParagraphBoundary() {
        String para1 = "a".repeat(1200);
        String para2 = "b".repeat(1200);
        String text = para1 + "\n\n" + para2;

        List<String> chunks = chunkingService.chunkText(text, 2000, 200);

        assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(chunks.get(0)).doesNotContain("b");
    }

    @Test
    void countTokens_spaceDelimited_returnsWordCount() {
        assertThat(chunkingService.countTokens("hello world foo")).isEqualTo(3);
    }
}
