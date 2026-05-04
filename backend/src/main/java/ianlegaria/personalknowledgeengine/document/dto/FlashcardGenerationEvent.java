package ianlegaria.personalknowledgeengine.document.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardGenerationEvent {
    private UUID documentId;
    private UUID userId;
    private String mode;
    private Integer numCards;
}
