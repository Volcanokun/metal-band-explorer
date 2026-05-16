package com.metalexplorer.infrastructure.config;

import org.springframework.context.annotation.Configuration;

// CB / リトライ / タイムリミッター / バルクヘッドは application.yml で設定済み。
// Resilience4j Spring Boot Starter が自動的に全ビーンを登録する。
@Configuration
public class ResilienceConfig {
}
