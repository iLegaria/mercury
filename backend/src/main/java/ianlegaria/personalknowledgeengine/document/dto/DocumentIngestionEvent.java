package ianlegaria.personalknowledgeengine.document.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentIngestionEvent {

    private UUID documentId;
    private UUID userId;
    private String title;
    private String sourceType;
    private String filePath;
    private String textContent;
}