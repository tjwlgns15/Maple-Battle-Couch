package com.battlecoach.ranker.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.battlecoach.diagnosis.domain.AnalysisContext;
import com.battlecoach.ranker.domain.RankerSample;
import com.battlecoach.ranker.repository.RankerSampleRepository;
import com.battlecoach.replay.application.AnalysisContextFactory;
import com.battlecoach.replay.application.ReplayQueryService;
import com.battlecoach.replay.domain.ReplayId;

import lombok.RequiredArgsConstructor;

/** 저장된 랭커 표본을 분석 컨텍스트로 읽는다. API 를 부르지 않는다. */
@Component
@RequiredArgsConstructor
class RankerSampleLoader {

    private final RankerSampleRepository rankerSampleRepository;
    private final ReplayQueryService replayQueryService;
    private final AnalysisContextFactory analysisContextFactory;

    /**
     * @param excludeReplayId 뺄 리플레이(진단 대상 등). 없으면 빈 문자열
     * @return 캐릭터명 → 분석 컨텍스트 (수집 순서)
     */
    Map<String, AnalysisContext> load(String characterClass, int periodNo, String excludeReplayId) {
        Map<String, AnalysisContext> samples = new LinkedHashMap<>();
        for (RankerSample sample : rankerSampleRepository.findByCharacterClassAndPeriodNo(characterClass, periodNo)) {
            if (sample.getReplayId().equals(excludeReplayId)) {
                continue;
            }
            replayQueryService.findStored(ReplayId.of(sample.getReplayId()))
                    .map(analysisContextFactory::create)
                    .ifPresent(context -> samples.put(sample.getCharacterName(), context));
        }
        return samples;
    }
}
