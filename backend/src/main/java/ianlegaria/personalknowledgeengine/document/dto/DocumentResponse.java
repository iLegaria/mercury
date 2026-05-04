package ianlegaria.personalknowledgeengine.document.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponse {

    private UUID id;
    private UUID userId;
    private String title;
    private String sourceType;
    private String status;
    private OffsetDateTime createdAt;
    private UUID collectionId;
    private String collectionName;
    private String extractedText;
}