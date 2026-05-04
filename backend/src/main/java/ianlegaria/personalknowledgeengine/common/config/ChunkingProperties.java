package ianlegaria.personalknowledgeengine.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.chunking")
public class ChunkingProperties {
    private int chunkSize = 2000;
    private int overlap = 200;
}
