package com.battlecoach.replay.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.nexon.NexonApiException;
import com.battlecoach.nexon.dto.CharacterInfoBasicResponse;
import com.battlecoach.nexon.dto.RawResponse;
import com.battlecoach.replay.application.ReplayRefreshService.RefreshResult;
import com.battlecoach.replay.domain.CharacterProfile;
import com.battlecoach.replay.repository.ReplayRepository;

class ReplayRefreshServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final ReplayRepository replayRepository = mock(ReplayRepository.class);
    private final NexonApiClient nexonApiClient = mock(NexonApiClient.class);
    private final ReplayRefreshWriter writer = mock(ReplayRefreshWriter.class);
    private ReplayRefreshService service;

    @BeforeEach
    void setUp() {
        ReplayRefreshProperties properties = new ReplayRefreshProperties(true, "0 0 4 * * *", Duration.ofDays(25), 100);
        service = new ReplayRefreshService(replayRepository, nexonApiClient, writer, properties, CLOCK);
    }

    @Test
    void 조회되면_캐릭터_정보를_바꾸고_조회되지_않으면_지운다() {
        when(replayRepository.findStale(eq(LocalDateTime.now(CLOCK).minusDays(25)), any())).thenReturn(List.of("a", "b"));
        when(nexonApiClient.findCharacterInfo("a")).thenReturn(RawResponse.of(basic("새이름"), "{}"));
        when(nexonApiClient.findCharacterInfo("b"))
                .thenThrow(NexonApiException.of(400, "OPENAPI00004", "Please input valid parameter"));

        RefreshResult result = service.refreshStale(10);

        assertThat(result).isEqualTo(new RefreshResult(2, 1, 1, null));
        verify(writer).apply(eq("a"), eq(CharacterProfile.of("새이름", "칼리", 290)), eq("{}"), any());
        verify(writer).delete("b");
    }

    @Test
    void 호출량_초과나_서버_오류면_지우지_않고_멈춘다() {
        when(replayRepository.findStale(any(), any())).thenReturn(List.of("a", "b"));
        when(nexonApiClient.findCharacterInfo("a")).thenThrow(NexonApiException.of(429, "OPENAPI00007", "rate"));

        RefreshResult result = service.refreshStale(10);

        assertThat(result.refreshed()).isZero();
        assertThat(result.stopReason()).contains("429");
        verify(writer, never()).delete(any());
        verify(nexonApiClient, never()).findCharacterInfo("b");
    }

    private static CharacterInfoBasicResponse basic(String name) {
        return new CharacterInfoBasicResponse(new CharacterInfoBasicResponse.Basic(name, 290, "칼리", "6"));
    }
}
