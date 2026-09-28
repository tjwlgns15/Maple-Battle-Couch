package com.battlecoach.nexon;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class NexonClientConfig {

    private static final String API_KEY_HEADER = "x-nxopen-api-key";

    @Bean
    public NexonRateLimiter nexonRateLimiter(NexonApiProperties properties) {
        return new NexonRateLimiter(properties.permitsPerSecond());
    }

    @Bean
    public NexonApiClient nexonApiClient(RestClient.Builder builder, JsonMapper jsonMapper,
                                         NexonRateLimiter rateLimiter, NexonApiProperties properties) {
        RestClient restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(API_KEY_HEADER, properties.apiKey())
                .build();
        return new RestClientNexonApiClient(restClient, jsonMapper, rateLimiter, properties);
    }
}
