package ianlegaria.personalknowledgeengine.chunk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Query is required")
    private String query;

    private Integer limit;

    private UUID collectionId; // null = search all collections
}