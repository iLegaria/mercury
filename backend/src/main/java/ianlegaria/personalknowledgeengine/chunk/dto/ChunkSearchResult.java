package ianlegaria.personalknowledgeengine.chunk.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkSearchResult {

    private UUID chunkId;
    private UUID documentId;
    private String documentTitle;
    private String content;
    private int chunkIndex;
    private double similarity;
}