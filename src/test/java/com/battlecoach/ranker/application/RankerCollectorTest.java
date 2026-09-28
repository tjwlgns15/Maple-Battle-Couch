package com.battlecoach.ranker.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.nexon.NexonApiException;
import com.battlecoach.nexon.dto.OverallRankingResponse;
import com.battlecoach.ranker.domain.RankerProbe;
import com.battlecoach.ranker.domain.RankerSample;
import com.battlecoach.ranker.repository.RankerProbeRepository;
import com.battlecoach.ranker.repository.RankerSampleRepository;
import com.battlecoach.replay.application.CharacterReplayService;
import com.battlecoach.replay.application.ReplayPeriodRecorder;
import com.battlecoach.replay.application.ReplayQueryService;
import com.battlecoach.replay.application.dto.ReplayDetail;
import com.battlecoach.replay.application.dto.ReplayListItem;
import com.battlecoach.replay.domain.ReplayId;

class RankerCollectorTest {

    private static final String JOB = "칼리-전체전직";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 27);

    private NexonApiClient nexon;
    private CharacterReplayService characterReplayService;
    private ReplayQueryService replayQueryService;
    private RankerProbeRepository probeRepository;
    private RankerSampleRepository sampleRepository;
    private RankerCollector collector;

    @BeforeEach
    void setUp() {
        nexon = mock(NexonApiClient.class);
        characterReplayService = mock(CharacterReplayService.class);
        replayQueryService = mock(ReplayQueryService.class);
        probeRepository = mock(RankerProbeRepository.class);
        sampleRepository = mock(RankerSampleRepository.class);
        collector = new RankerCollector(nexon, characterReplayService, mock(ReplayPeriodRecorder.class),
                replayQueryService, probeRepository, sampleRepository, new SyncTaskExecutor(),
                Clock.fixed(Instant.parse("2026-09-28T00:00:00Z"), ZoneId.of("Asia/Seoul")));
    }

    @Test
    void 대상_기간_기록은_표본으로_저장하고_기록이_없거나_이미_확인한_랭커는_그에_맞게_처리한다() {
        ranking("이미확인", "기록없음", "표본");
        when(probeRepository.existsByJobClassAndCharacterNameAndPeriodNo(JOB, "이미확인", 4)).thenReturn(true);
        when(nexon.findOcid(anyString())).thenAnswer(invocation -> "ocid-" + invocation.getArgument(0));
        when(characterReplayService.findByOcid("ocid-기록없음"))
                .thenThrow(NexonApiException.of(400, "OPENAPI00004", "Please input valid parameter"));
        when(characterReplayService.findByOcid("ocid-표본")).thenReturn(List.of(
                ReplayListItem.of(4, DATE, "r4"), ReplayListItem.of(1, DATE, "r1")));
        when(replayQueryService.findStored(ReplayId.of("r4"))).thenReturn(Optional.empty());
        when(replayQueryService.getReplay(ReplayId.of("r4"))).thenReturn(detail("r4"));

        CollectStatus status = run(400);

        assertThat(status.running()).isFalse();
        assertThat(status.rankersSkipped()).isEqualTo(1);
        assertThat(status.noRecord()).isEqualTo(1);
        assertThat(status.sampled()).isEqualTo(1);
        assertThat(status.callsUsed()).isEqualTo(1 + 2 + 2 + 3); // 랭킹 + 확인 2명 + 표본 본문
        verify(sampleRepository).save(any(RankerSample.class));
        verify(nexon, never()).findOcid("이미확인");
    }

    @Test
    void 다음_단계_호출이_상한을_넘으면_그_전에_멈춘다() {
        ranking("첫째", "둘째");
        when(nexon.findOcid(anyString())).thenReturn("ocid");
        when(characterReplayService.findByOcid("ocid"))
                .thenThrow(NexonApiException.of(400, "OPENAPI00004", "Please input valid parameter"));

        CollectStatus status = run(4); // 랭킹 1 + 첫째 2 = 3, 둘째는 2건이 필요해 멈춘다

        assertThat(status.callsUsed()).isEqualTo(3);
        assertThat(status.rankersChecked()).isEqualTo(1);
        assertThat(status.stopReason()).isEqualTo("호출 상한 도달");
        verify(probeRepository).save(any(RankerProbe.class));
    }

    @Test
    void 표본_수_상한에_닿으면_멈춘다() {
        ranking("sample1", "sample2");
        when(nexon.findOcid(anyString())).thenAnswer(invocation -> "ocid" + invocation.getArgument(0));
        when(characterReplayService.findByOcid(anyString())).thenAnswer(invocation -> List.of(
                ReplayListItem.of(4, DATE, "r" + invocation.getArgument(0, String.class).substring(4))));
        when(replayQueryService.findStored(any())).thenReturn(Optional.empty());
        when(replayQueryService.getReplay(any())).thenAnswer(invocation -> detail(invocation.getArgument(0, ReplayId.class).value()));

        collector.start(new CollectRequest(JOB, 4, DATE, 400, 200, 1));

        assertThat(collector.status().sampled()).isEqualTo(1);
        assertThat(collector.status().stopReason()).isEqualTo("표본 수 상한 도달");
        verify(nexon, never()).findOcid("sample2");
    }

    private CollectStatus run(int maxCalls) {
        collector.start(new CollectRequest(JOB, 4, DATE, maxCalls, 200, null)); // SyncTaskExecutor 라 끝까지 실행된다
        return collector.status();
    }

    private void ranking(String... names) {
        List<OverallRankingResponse.Entry> entries = java.util.stream.IntStream.range(0, names.length)
                .mapToObj(i -> new OverallRankingResponse.Entry(i + 1, names[i], 299, "칼리", "", "루나"))
                .toList();
        when(nexon.findOverallRanking(eq(DATE), eq(JOB), anyInt())).thenReturn(new OverallRankingResponse(entries));
    }

    private static ReplayDetail detail(String replayId) {
        return new ReplayDetail(replayId, "표본", "칼리", 299, DATE, 300_000, 1, 1, "0",
                List.of(), List.of(), List.of());
    }
}
