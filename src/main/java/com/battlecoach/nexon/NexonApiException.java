package com.battlecoach.nexon;

import lombok.Getter;

/**
 * Nexon Open API 오류 응답.
 * 응답 본문 형식: {"error": {"name": "OPENAPI00007", "message": "..."}}
 */
@Getter
public class NexonApiException extends RuntimeException {

    private static final int TOO_MANY_REQUESTS = 429;

    private final int httpStatus;
    private final String errorName;

    private NexonApiException(int httpStatus, String errorName, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorName = errorName;
    }

    public static NexonApiException of(int httpStatus, String errorName, String message) {
        return new NexonApiException(httpStatus, errorName, message);
    }

    /** 호출량 초과(429)와 서버 오류(5xx)만 재시도 대상이다. 4xx는 요청 자체가 잘못된 것이라 재시도해도 같다. */
    public boolean isRetryable() {
        return httpStatus == TOO_MANY_REQUESTS || httpStatus >= 500;
    }

    public boolean isRateLimited() {
        return httpStatus == TOO_MANY_REQUESTS;
    }

    public boolean isClientError() {
        return httpStatus >= 400 && httpStatus < 500 && !isRateLimited();
    }
}
