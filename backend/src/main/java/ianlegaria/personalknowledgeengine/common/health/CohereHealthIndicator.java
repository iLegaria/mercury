package ianlegaria.personalknowledgeengine.common.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CohereHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Health health() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("cohereAPI");
        CircuitBreaker.State state = cb.getState();
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        return switch (state) {
            case CLOSED -> Health.up()
                    .withDetail("state", "CLOSED")
                    .withDetail("failureRate", metrics.getFailureRate() + "%")
                    .build();
            case OPEN -> Health.down()
                    .withDetail("state", "OPEN")
                    .withDetail("reason", "Cohere API failure threshold exceeded")
                    .withDetail("failureRate", metrics.getFailureRate() + "%")
                    .build();
            case HALF_OPEN -> Health.unknown()
                    .withDetail("state", "HALF_OPEN")
                    .withDetail("bufferedCalls", metrics.getNumberOfBufferedCalls())
                    .build();
            default -> Health.unknown()
                    .withDetail("state", state.name())
                    .build();
        };
    }
}
