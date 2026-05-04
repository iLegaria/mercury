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
public class AskRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Question is required")
    private String question;

    private UUID collectionId; // null = ask across all collections
}