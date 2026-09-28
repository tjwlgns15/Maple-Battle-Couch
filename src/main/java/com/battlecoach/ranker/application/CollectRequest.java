package com.battlecoach.ranker.application;

import java.time.LocalDate;

/**
 * @param jobClass    랭킹 직업 파라미터. 예: "칼리-전체전직", "마법사-비숍"
 * @param periodNo    표본으로 삼을 연무장 기간. null 이면 지금까지 본 가장 최근 기간
 * @param rankingDate 랭킹 기준일. null 이면 어제(오늘 랭킹은 아직 집계 전일 수 있다)
 * @param maxCalls    이번 실행에서 쓸 API 호출 상한
 * @param maxRankers  이번 실행에서 새로 확인할 랭커 수 상한
 * @param maxSamples  이번 실행에서 새로 모을 표본 수 상한. null 이면 제한 없음
 */
public record CollectRequest(
        String jobClass,
        Integer periodNo,
        LocalDate rankingDate,
        int maxCalls,
        int maxRankers,
        Integer maxSamples
) {

    /** 개발 키 하루 한도(1,000건) 안에서 다른 조회에 쓸 몫을 남긴다. */
    public static final int MAX_CALLS_LIMIT = 800;

    public CollectRequest {
        if (jobClass == null || jobClass.isBlank()) {
            throw new IllegalArgumentException("직업(jobClass)을 입력해 주세요. 예: 칼리-전체전직");
        }
        if (maxCalls <= 0 || maxCalls > MAX_CALLS_LIMIT) {
            throw new IllegalArgumentException("maxCalls 는 1~" + MAX_CALLS_LIMIT + " 사이여야 합니다.");
        }
        if (maxRankers <= 0) {
            throw new IllegalArgumentException("maxRankers 는 1 이상이어야 합니다.");
        }
        if (maxSamples != null && maxSamples <= 0) {
            throw new IllegalArgumentException("maxSamples 는 1 이상이어야 합니다.");
        }
        jobClass = jobClass.strip();
    }
}
