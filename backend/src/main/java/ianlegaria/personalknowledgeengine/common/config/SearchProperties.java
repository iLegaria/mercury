package ianlegaria.personalknowledgeengine.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.search")
public class SearchProperties {
    private int cacheTtlMinutes = 10;
}
