package com.battlecoach.nexon;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nexon.api")
public record NexonApiProperties(
        String baseUrl,
        String apiKey,
        int permitsPerSecond,
        int maxAttempts,
        Duration initialBackoff
) {

    public NexonApiProperties {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("nexon.api.api-key 가 설정되지 않았습니다. (환경변수 NEXON_API_KEY)");
        }
        if (permitsPerSecond <= 0) {
            throw new IllegalStateException("nexon.api.permits-per-second 는 1 이상이어야 합니다.");
        }
        if (maxAttempts <= 0) {
            throw new IllegalStateException("nexon.api.max-attempts 는 1 이상이어야 합니다.");
        }
    }
}
