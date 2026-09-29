package com.battlecoach.ranker.application;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.battlecoach.diagnosis.statistics.HoldTradeoff;
import com.battlecoach.diagnosis.statistics.HoldTradeoffAnalyzer;

import lombok.RequiredArgsConstructor;

/** 랭커 표본으로 "극딜까지 아끼는 것 vs 쿨마다 쓰는 것" 가설을 확인한다(분석용). */
@Service
@RequiredArgsConstructor
public class HoldTradeoffService {

    private static final String NO_EXCLUSION = "";

    private final RankerSampleLoader rankerSampleLoader;
    private final HoldTradeoffAnalyzer holdTradeoffAnalyzer;

    public Optional<HoldTradeoff> analyze(String characterClass, int periodNo, String skillBaseName) {
        return holdTradeoffAnalyzer.analyze(skillBaseName,
                rankerSampleLoader.load(characterClass, periodNo, NO_EXCLUSION));
    }
}
