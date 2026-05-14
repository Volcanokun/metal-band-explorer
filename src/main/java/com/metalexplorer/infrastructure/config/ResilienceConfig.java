package com.metalexplorer.infrastructure.config;

import org.springframework.context.annotation.Configuration;

// Circuit breaker / retry / timelimiter / bulkhead instances are fully configured
// via application.yml. Resilience4j Spring Boot starter auto-registers all beans.
@Configuration
public class ResilienceConfig {
}
