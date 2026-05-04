package ianlegaria.personalknowledgeengine.collection.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private OffsetDateTime createdAt;
}
