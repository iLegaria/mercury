package ianlegaria.personalknowledgeengine.whatsapp.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppSession {
    private UUID userId;
    private List<UUID> cardIds;
    private int currentIndex;
}
