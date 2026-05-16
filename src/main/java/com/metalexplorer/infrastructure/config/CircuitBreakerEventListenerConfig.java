package com.metalexplorer.infrastructure.config;

import com.metalexplorer.service.MetricsService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
@RequiredArgsConstructor
@Slf4j
public class CircuitBreakerEventListenerConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MetricsService metricsService;

    @PostConstruct
    void registerEventListeners() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("lastfmApi");

        cb.getEventPublisher()
                .onStateTransition(event -> {
                    var t = event.getStateTransition();
                    log.warn("CircuitBreaker state transition",
                            kv("cbName", event.getCircuitBreakerName()),
                            kv("fromState", t.getFromState().name()),
                            kv("toState", t.getToState().name()));
                    metricsService.recordApiCall("stateTransition", false);
                })
                .onError(event -> log.debug("CircuitBreaker recorded error",
                        kv("cbName", event.getCircuitBreakerName()),
                        kv("elapsedMs", event.getElapsedDuration().toMillis())))
                .onCallNotPermitted(event -> log.warn("CircuitBreaker call rejected - CB is OPEN or HALF_OPEN",
                        kv("cbName", event.getCircuitBreakerName())));
    }
}
