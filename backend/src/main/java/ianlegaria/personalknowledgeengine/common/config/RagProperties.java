package ianlegaria.personalknowledgeengine.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {
    private int maxChunks = 5;
    private double minSimilarity = 0.3;
}
