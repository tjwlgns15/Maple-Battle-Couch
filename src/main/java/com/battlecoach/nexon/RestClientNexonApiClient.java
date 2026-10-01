package com.battlecoach.nexon;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Function;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.battlecoach.nexon.dto.BattlePracticeResultResponse;
import com.battlecoach.nexon.dto.CharacterBasicResponse;
import com.battlecoach.nexon.dto.CharacterInfoBasicResponse;
import com.battlecoach.nexon.dto.NexonErrorResponse;
import com.battlecoach.nexon.dto.OcidResponse;
import com.battlecoach.nexon.dto.RawResponse;
import com.battlecoach.nexon.dto.ReplayIdListResponse;
import com.battlecoach.nexon.dto.SkillTimelineResponse;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class RestClientNexonApiClient implements NexonApiClient {

    private static final int NETWORK_ERROR_STATUS = 503;
    private static final String NETWORK_ERROR_NAME = "NETWORK_ERROR";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final NexonRateLimiter rateLimiter;
    private final int maxAttempts;
    private final Duration initialBackoff;

    public RestClientNexonApiClient(RestClient restClient, JsonMapper jsonMapper,
                                    NexonRateLimiter rateLimiter, NexonApiProperties properties) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
        this.rateLimiter = rateLimiter;
        this.maxAttempts = properties.maxAttempts();
        this.initialBackoff = properties.initialBackoff();
    }

    @Override
    public String findOcid(String characterName) {
        String json = get(uri -> uri.path("/maplestory/v1/id")
                .queryParam("character_name", characterName)
                .build());
        return parse(json, OcidResponse.class).ocid();
    }

    @Override
    public CharacterBasicResponse findCharacterBasic(String ocid) {
        String json = get(uri -> uri.path("/maplestory/v1/character/basic")
                .queryParam("ocid", ocid)
                .build());
        return parse(json, CharacterBasicResponse.class);
    }

    @Override
    public ReplayIdListResponse findReplayIds(String ocid) {
        String json = get(uri -> uri.path("/maplestory/v1/battle-practice/replay-id")
                .queryParam("ocid", ocid)
                .build());
        return parse(json, ReplayIdListResponse.class);
    }

    @Override
    public RawResponse<BattlePracticeResultResponse> findResult(String replayId) {
        String json = get(uri -> uri.path("/maplestory/v1/battle-practice/result")
                .queryParam("replay_id", replayId)
                .build());
        return RawResponse.of(parse(json, BattlePracticeResultResponse.class), json);
    }

    @Override
    public RawResponse<SkillTimelineResponse> findSkillTimeline(String replayId, int pageNo) {
        String json = get(uri -> uri.path("/maplestory/v1/battle-practice/skill-timeline")
                .queryParam("replay_id", replayId)
                .queryParam("page_no", pageNo)
                .build());
        return RawResponse.of(parse(json, SkillTimelineResponse.class), json);
    }

    @Override
    public RawResponse<CharacterInfoBasicResponse> findCharacterInfo(String replayId) {
        String json = get(uri -> uri.path("/maplestory/v1/battle-practice/character-info")
                .queryParam("replay_id", replayId)
                .build());
        return RawResponse.of(parse(json, CharacterInfoBasicResponse.class), json);
    }


    private String get(Function<UriBuilder, URI> uriFunction) {
        for (int attempt = 1; ; attempt++) {
            rateLimiter.acquire();
            try {
                return restClient.get()
                        .uri(uriFunction)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, this::throwNexonError)
                        .body(String.class);
            } catch (NexonApiException e) {
                if (!e.isRetryable() || attempt >= maxAttempts) {
                    throw e;
                }
                log.warn("Nexon API 재시도 {}/{} - status={}, error={}", attempt, maxAttempts, e.getHttpStatus(), e.getErrorName());
            } catch (ResourceAccessException e) {
                if (attempt >= maxAttempts) {
                    throw NexonApiException.of(NETWORK_ERROR_STATUS, NETWORK_ERROR_NAME, e.getMessage());
                }
                log.warn("Nexon API 네트워크 오류, 재시도 {}/{} - {}", attempt, maxAttempts, e.getMessage());
            }
            backoff(attempt);
        }
    }

    private void throwNexonError(HttpRequest request, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        NexonErrorResponse.Error error = parseErrorOrNull(body);
        if (error == null) {
            throw NexonApiException.of(status, "UNKNOWN", body);
        }
        throw NexonApiException.of(status, error.name(), error.message());
    }

    private NexonErrorResponse.Error parseErrorOrNull(String body) {
        try {
            NexonErrorResponse response = jsonMapper.readValue(body, NexonErrorResponse.class);
            return response.error();
        } catch (JacksonException e) {
            return null;
        }
    }

    private <T> T parse(String json, Class<T> type) {
        return jsonMapper.readValue(json, type);
    }

    /** 지수 백오프: 500ms, 1s, 2s ... */
    private void backoff(int attempt) {
        long millis = initialBackoff.toMillis() * (1L << (attempt - 1));
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Nexon API 재시도 대기 중 인터럽트되었습니다.", e);
        }
    }
}
