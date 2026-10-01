package com.battlecoach.replay.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.battlecoach.nexon.NexonApiException;
import com.battlecoach.replay.application.CharacterNotFoundException;

import lombok.extern.slf4j.Slf4j;

/** 화면 요청의 오류를 JSON 대신 오류 페이지로 보여준다. API 오류는 GlobalExceptionHandler 가 맡는다. */
@Slf4j
@ControllerAdvice(basePackageClasses = PageExceptionHandler.class)
public class PageExceptionHandler {

    private static final String ERROR_VIEW = "error";

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgument(IllegalArgumentException e) {
        return errorPage(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CharacterNotFoundException.class)
    public ModelAndView handleCharacterNotFound(CharacterNotFoundException e) {
        return errorPage(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NexonApiException.class)
    public ModelAndView handleNexonApi(NexonApiException e) {
        log.warn("Nexon API 오류 - status={}, error={}, message={}", e.getHttpStatus(), e.getErrorName(), e.getMessage());
        if (e.isRateLimited()) {
            return errorPage(HttpStatus.SERVICE_UNAVAILABLE, "요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
        if (e.isClientError()) {
            return errorPage(HttpStatus.BAD_REQUEST, "캐릭터나 기록을 찾을 수 없습니다. 이름을 확인해 주세요.");
        }
        return errorPage(HttpStatus.BAD_GATEWAY, "넥슨 API 응답에 문제가 있습니다. 잠시 후 다시 시도해 주세요.");
    }

    private static ModelAndView errorPage(HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView(ERROR_VIEW, status);
        mav.addObject("status", status.value());
        mav.addObject("message", message);
        return mav;
    }
}
