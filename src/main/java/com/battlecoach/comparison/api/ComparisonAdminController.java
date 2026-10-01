package com.battlecoach.comparison.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.battlecoach.comparison.application.HoldTradeoffService;
import com.battlecoach.comparison.application.JobStatisticsService;
import com.battlecoach.diagnosis.statistics.ComparisonCriteria;
import com.battlecoach.diagnosis.statistics.ComparisonStatistics;
import com.battlecoach.diagnosis.statistics.HoldTradeoff;
import com.battlecoach.diagnosis.statistics.JobStatistics;

import lombok.RequiredArgsConstructor;

/**
 * 비교 통계 확인·분석 API. 인증이 없으므로 admin.enabled=true 일 때만 등록한다(로컬 전용).
 * 비교 조건(basis, top)은 상세 화면과 같다. 없으면 DPS 상위 50%.
 */
@RestController
@RequestMapping("/api/admin/comparison")
@ConditionalOnProperty(prefix = "admin", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ComparisonAdminController {

    private static final String NO_EXCLUSION = "";

    private final JobStatisticsService jobStatisticsService;
    private final HoldTradeoffService holdTradeoffService;

    /** 예: GET /api/admin/comparison/statistics?characterClass=칼리&periodNo=4&basis=EFFICIENCY&top=25 */
    @GetMapping("/statistics")
    public JobStatistics statistics(@RequestParam String characterClass, @RequestParam int periodNo,
                                    @RequestParam(required = false) String basis,
                                    @RequestParam(required = false) Integer top) {
        return jobStatisticsService.find(characterClass, periodNo, NO_EXCLUSION, ComparisonCriteria.parse(basis, top))
                .flatMap(ComparisonStatistics::jobStatistics)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "표본이 없습니다."));
    }

    /**
     * "극딜까지 아끼는 것 vs 쿨마다 쓰는 것" 가설 확인 (분석용)
     * 예: GET /api/admin/comparison/hold-tradeoff?characterClass=칼리&periodNo=4&skill=헥스 : 판데모니움&top=100
     */
    @GetMapping("/hold-tradeoff")
    public HoldTradeoff holdTradeoff(@RequestParam String characterClass, @RequestParam int periodNo,
                                     @RequestParam String skill,
                                     @RequestParam(required = false) String basis,
                                     @RequestParam(required = false) Integer top) {
        return holdTradeoffService.analyze(characterClass, periodNo, skill, ComparisonCriteria.parse(basis, top))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이 스킬을 쓴 표본이 없습니다."));
    }
}
