package com.battlecoach.ranker.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.battlecoach.nexon.NexonApiClient;
import com.battlecoach.nexon.NexonApiException;
import com.battlecoach.nexon.dto.OverallRankingResponse;
import com.battlecoach.ranker.domain.ProbeResult;
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

import lombok.extern.slf4j.Slf4j;

/**
 * 종합 랭킹 순서대로 랭커를 확인해, 대상 기간의 연무장 기록을 표본으로 저장한다.
 * <ul>
 *   <li>랭커 1명 확인 = ocid 1건 + 기록 목록 1건. 표본이면 result·timeline·character-info 3건을 더 쓴다.</li>
 *   <li>확인 결과(기록 없음 포함)를 ranker_probe 에 남겨, 다시 실행해도 같은 랭커는 부르지 않는다.</li>
 *   <li>다음 단계에 필요한 호출 수가 상한을 넘으면 그 전에 멈춘다. 호출 간격은 NexonRateLimiter 가 지킨다.</li>
 * </ul>
 * 한 번에 하나만 실행하며 별도 스레드에서 돈다.
 */
@Slf4j
@Service
public class RankerCollector {

    private static final int CALLS_PER_PROBE = 2;
    private static final int CALLS_PER_REPLAY = 3;
    private static final int RANKING_PAGE_SIZE = 200;

    private final NexonApiClient nexonApiClient;
    private final CharacterReplayService characterReplayService;
    private final ReplayPeriodRecorder replayPeriodRecorder;
    private final ReplayQueryService replayQueryService;
    private final RankerProbeRepository rankerProbeRepository;
    private final RankerSampleRepository rankerSampleRepository;
    private final TaskExecutor taskExecutor;
    private final Clock clock;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile CollectStatus status = CollectStatus.idle();

    public RankerCollector(NexonApiClient nexonApiClient, CharacterReplayService characterReplayService,
                           ReplayPeriodRecorder replayPeriodRecorder, ReplayQueryService replayQueryService,
                           RankerProbeRepository rankerProbeRepository, RankerSampleRepository rankerSampleRepository,
                           TaskExecutor taskExecutor, Clock clock) {
        this.nexonApiClient = nexonApiClient;
        this.characterReplayService = characterReplayService;
        this.replayPeriodRecorder = replayPeriodRecorder;
        this.replayQueryService = replayQueryService;
        this.rankerProbeRepository = rankerProbeRepository;
        this.rankerSampleRepository = rankerSampleRepository;
        this.taskExecutor = taskExecutor;
        this.clock = clock;
    }

    public CollectStatus status() {
        return status;
    }

    /** @throws IllegalStateException 이미 실행 중일 때 */
    public CollectStatus start(CollectRequest request) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("이미 수집이 실행 중입니다.");
        }
        try {
            int periodNo = Optional.ofNullable(request.periodNo())
                    .orElseGet(() -> replayPeriodRecorder.latestPeriod().orElseThrow(() ->
                            new IllegalArgumentException("기간을 알 수 없습니다. periodNo 를 지정해 주세요.")));
            LocalDate rankingDate = Optional.ofNullable(request.rankingDate())
                    .orElseGet(() -> LocalDate.now(clock).minusDays(1));
            Run run = new Run(request, periodNo, rankingDate, LocalDateTime.now(clock));
            status = run.snapshot(true);
            taskExecutor.execute(() -> execute(run));
            return status;
        } catch (RuntimeException e) {
            running.set(false);
            throw e;
        }
    }

    private void execute(Run run) {
        try {
            run.stopReason = collect(run);
        } catch (NexonApiException e) {
            run.stopReason = "Nexon API 오류로 중단: " + e.getHttpStatus() + " " + e.getErrorName() + " " + e.getMessage();
            log.warn("랭커 수집 중단 - {}", run.stopReason);
        } catch (RuntimeException e) {
            run.stopReason = "예상하지 못한 오류로 중단: " + e.getMessage();
            log.error("랭커 수집 중단", e);
        } finally {
            run.finishedAt = LocalDateTime.now(clock);
            status = run.snapshot(false);
            running.set(false);
            log.info("랭커 수집 종료 - {}", status);
        }
    }

    private String collect(Run run) {
        for (int page = 1; ; page++) {
            if (!run.canSpend(1)) {
                return "호출 상한 도달";
            }
            run.callsUsed++;
            List<OverallRankingResponse.Entry> entries =
                    nexonApiClient.findOverallRanking(run.rankingDate, run.request.jobClass(), page).entries();
            for (OverallRankingResponse.Entry entry : entries) {
                if (run.checked >= run.request.maxRankers()) {
                    return "랭커 수 상한 도달";
                }
                if (run.request.maxSamples() != null && run.sampled >= run.request.maxSamples()) {
                    return "표본 수 상한 도달";
                }
                if (rankerProbeRepository.existsByJobClassAndCharacterNameAndPeriodNo(
                        run.request.jobClass(), entry.characterName(), run.periodNo)) {
                    run.skipped++;
                    continue;
                }
                if (!run.canSpend(CALLS_PER_PROBE)) {
                    return "호출 상한 도달";
                }
                if (!probe(run, entry.characterName())) {
                    return "호출 상한 도달";
                }
                status = run.snapshot(true);
            }
            if (entries.size() < RANKING_PAGE_SIZE) {
                return "랭킹 끝까지 확인";
            }
        }
    }

    /** @return false 면 표본을 받을 호출이 모자라 멈춰야 한다(이 랭커는 기록하지 않아 다음 실행에서 다시 본다). */
    private boolean probe(Run run, String characterName) {
        String ocid;
        try {
            run.callsUsed++;
            ocid = nexonApiClient.findOcid(characterName);
        } catch (NexonApiException e) {
            if (!e.isClientError()) {
                throw e;
            }
            record(run, characterName, ProbeResult.NOT_FOUND);
            return true;
        }

        List<ReplayListItem> replays;
        try {
            run.callsUsed++;
            replays = characterReplayService.findByOcid(ocid);
        } catch (NexonApiException e) {
            if (!e.isClientError()) {
                throw e;
            }
            // 기록이 없는 캐릭터는 빈 목록이 아니라 400 OPENAPI00004 가 온다(칼리 랭커 20명 중 13명).
            record(run, characterName, ProbeResult.NO_RECORD);
            return true;
        }
        replayPeriodRecorder.record(replays);

        Optional<ReplayListItem> target = replays.stream()
                .filter(item -> item.periodNo() == run.periodNo)
                .findFirst();
        if (target.isEmpty()) {
            record(run, characterName, ProbeResult.OTHER_PERIOD_ONLY);
            return true;
        }

        ReplayId replayId = ReplayId.of(target.get().replayId());
        boolean stored = replayQueryService.findStored(replayId).isPresent();
        if (!stored && !run.canSpend(CALLS_PER_REPLAY)) {
            return false;
        }
        if (!stored) {
            run.callsUsed += CALLS_PER_REPLAY; // 타임라인이 여러 페이지면 더 쓰지만 지금까지 모두 1페이지였다.
        }
        ReplayDetail replay = replayQueryService.getReplay(replayId);
        saveSample(run, replay, characterName);
        record(run, characterName, ProbeResult.SAMPLED);
        return true;
    }

    private void saveSample(Run run, ReplayDetail replay, String characterName) {
        if (rankerSampleRepository.existsById(replay.replayId())) {
            return;
        }
        rankerSampleRepository.save(RankerSample.of(
                replay.replayId(), replay.characterClass(), run.periodNo, characterName, LocalDateTime.now(clock)));
    }

    private void record(Run run, String characterName, ProbeResult result) {
        run.checked++;
        switch (result) {
            case SAMPLED -> run.sampled++;
            case NO_RECORD -> run.noRecord++;
            case OTHER_PERIOD_ONLY -> run.otherPeriodOnly++;
            case NOT_FOUND -> run.notFound++;
        }
        try {
            rankerProbeRepository.save(RankerProbe.of(
                    run.request.jobClass(), characterName, run.periodNo, result, LocalDateTime.now(clock)));
        } catch (DataIntegrityViolationException e) {
            log.info("이미 확인한 랭커입니다: {}", characterName);
        }
    }

    /** 실행 중에만 쓰는 가변 상태. 수집 스레드 하나만 고친다. */
    private static final class Run {

        private final CollectRequest request;
        private final int periodNo;
        private final LocalDate rankingDate;
        private final LocalDateTime startedAt;
        private int callsUsed;
        private int checked;
        private int skipped;
        private int sampled;
        private int noRecord;
        private int otherPeriodOnly;
        private int notFound;
        private String stopReason;
        private LocalDateTime finishedAt;

        private Run(CollectRequest request, int periodNo, LocalDate rankingDate, LocalDateTime startedAt) {
            this.request = request;
            this.periodNo = periodNo;
            this.rankingDate = rankingDate;
            this.startedAt = startedAt;
        }

        private boolean canSpend(int calls) {
            return callsUsed + calls <= request.maxCalls();
        }

        private CollectStatus snapshot(boolean running) {
            return new CollectStatus(running, request.jobClass(), periodNo, rankingDate, callsUsed, request.maxCalls(),
                    checked, skipped, sampled, noRecord, otherPeriodOnly, notFound, stopReason, startedAt, finishedAt);
        }
    }
}
