package com.battlecoach.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.battlecoach.nexon.NexonApiException;

import lombok.extern.slf4j.Slf4j;

/** REST API 오류 응답. 화면 요청은 PageExceptionHandler 가 오류 페이지로 처리한다. */
@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_REQUEST", e.getMessage()));
    }

    /**
     * Nexon 4xx: 존재하지 않는 캐릭터·잘못된 식별자 등 요청 문제 → 400
     * Nexon 429: 호출량 초과 → 503 (잠시 후 재시도)
     * Nexon 5xx / 네트워크: 외부 서비스 장애 → 502
     */
    @ExceptionHandler(NexonApiException.class)
    public ResponseEntity<ErrorResponse> handleNexonApi(NexonApiException e) {
        log.warn("Nexon API 오류 - status={}, error={}, message={}", e.getHttpStatus(), e.getErrorName(), e.getMessage());
        HttpStatus status = toHttpStatus(e);
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(e.getErrorName(), e.getMessage()));
    }

    private static HttpStatus toHttpStatus(NexonApiException e) {
        if (e.isRateLimited()) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (e.isClientError()) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_GATEWAY;
    }
}
