package ianlegaria.personalknowledgeengine.chunk.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RAGResponse {

    private String answer;
    private List<ChunkSearchResult> chunks;
}