package ianlegaria.personalknowledgeengine.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignCollectionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private UUID collectionId; // null = remove from collection
}
