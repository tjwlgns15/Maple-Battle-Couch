package com.battlecoach.nexon.dto;

/**
 * 역직렬화된 응답과 원문 JSON을 함께 담는다.
 * 원문은 파서/진단 규칙이 바뀌었을 때 API 재호출 없이 재분석하기 위해 저장한다.
 */
public record RawResponse<T>(T body, String rawJson) {

    public static <T> RawResponse<T> of(T body, String rawJson) {
        return new RawResponse<>(body, rawJson);
    }
}
