package com.battlecoach.ranker.api;

import java.time.LocalDate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.battlecoach.diagnosis.statistics.HoldTradeoff;
import com.battlecoach.diagnosis.statistics.JobStatistics;
import com.battlecoach.ranker.application.CollectRequest;
import com.battlecoach.ranker.application.CollectStatus;
import com.battlecoach.ranker.application.HoldTradeoffService;
import com.battlecoach.ranker.application.JobStatisticsService;
import com.battlecoach.ranker.application.RankerCollector;

import lombok.RequiredArgsConstructor;

/**
 * 랭커 수집 관리 API. 인증이 없으므로 collector.enabled=true 일 때만 등록한다(로컬 전용).
 * 예: POST /api/admin/rankers/collect?jobClass=칼리-전체전직&maxCalls=400&maxRankers=200&maxSamples=5
 */
@RestController
@RequestMapping("/api/admin/rankers")
@ConditionalOnProperty(prefix = "collector", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RankerAdminController {

    private static final String NO_EXCLUSION = "";

    private final RankerCollector rankerCollector;
    private final JobStatisticsService jobStatisticsService;
    private final HoldTradeoffService holdTradeoffService;

    @PostMapping("/collect")
    public ResponseEntity<CollectStatus> collect(
            @RequestParam String jobClass,
            @RequestParam(required = false) Integer periodNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rankingDate,
            @RequestParam(defaultValue = "400") int maxCalls,
            @RequestParam(defaultValue = "200") int maxRankers,
            @RequestParam(required = false) Integer maxSamples) {
        try {
            CollectStatus status = rankerCollector.start(
                    new CollectRequest(jobClass, periodNo, rankingDate, maxCalls, maxRankers, maxSamples));
            return ResponseEntity.accepted().body(status);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @GetMapping("/status")
    public CollectStatus status() {
        return rankerCollector.status();
    }

    /** 수집한 표본으로 계산한 통계 (확인용) */
    @GetMapping("/statistics")
    public JobStatistics statistics(@RequestParam String characterClass, @RequestParam int periodNo) {
        return jobStatisticsService.find(characterClass, periodNo, NO_EXCLUSION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "표본이 없습니다."));
    }

    /**
     * "극딜까지 아끼는 것 vs 쿨마다 쓰는 것" 가설 확인 (분석용)
     * 예: GET /api/admin/rankers/hold-tradeoff?characterClass=칼리&periodNo=4&skill=헥스 : 판데모니움
     */
    @GetMapping("/hold-tradeoff")
    public HoldTradeoff holdTradeoff(@RequestParam String characterClass, @RequestParam int periodNo,
                                     @RequestParam String skill) {
        return holdTradeoffService.analyze(characterClass, periodNo, skill)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이 스킬을 쓴 표본이 없습니다."));
    }
}
