package com.metalexplorer.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    void registerGauges() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("lastfmApi");
        Gauge.builder("circuitbreaker.state", cb, this::toNumericState)
                .tag("name", "lastfmApi")
                .description("0=CLOSED 1=OPEN 2=HALF_OPEN")
                .register(meterRegistry);
    }

    public void recordApiCall(String method, boolean success) {
        Counter.builder("lastfm.api.call.total")
                .tag("method", method)
                .tag("status", success ? "success" : "failure")
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample startLatencySample() {
        return Timer.start(meterRegistry);
    }

    public void stopLatencySample(Timer.Sample sample, String method) {
        sample.stop(Timer.builder("lastfm.api.latency")
                .tag("method", method)
                .publishPercentileHistogram()
                .register(meterRegistry));
    }

    public void recordCacheHit(String source) {
        Counter.builder("artist.cache.hit")
                .tag("source", source)
                .register(meterRegistry)
                .increment();
    }

    private double toNumericState(CircuitBreaker cb) {
        return switch (cb.getState()) {
            case CLOSED -> 0;
            case OPEN -> 1;
            case HALF_OPEN -> 2;
            default -> -1;
        };
    }
}
