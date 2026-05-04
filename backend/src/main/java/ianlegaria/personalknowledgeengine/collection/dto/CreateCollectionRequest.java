package ianlegaria.personalknowledgeengine.collection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCollectionRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Collection name is required")
    @Size(max = 200, message = "Name must be 200 characters or fewer")
    private String name;
}
