package com.battlecoach.replay.web;

import java.util.Locale;

import org.springframework.stereotype.Component;

/** 템플릿용 시간 표기. {@code ${@timeFormat.seconds(ms)}} 처럼 쓴다. 값이 없으면 "-". */
@Component
public class TimeFormat {

    private static final String EMPTY = "-";

    /** ms → "23.5초" */
    public String seconds(Long ms) {
        if (ms == null) {
            return EMPTY;
        }
        return String.format(Locale.ROOT, "%.1f초", ms / 1000.0);
    }

    /** 초 → "23.5" */
    public String decimal(Double seconds) {
        return seconds == null ? EMPTY : String.format(Locale.ROOT, "%.1f", seconds);
    }

    /** 초 → "+1.2", "-0.4" */
    public String signed(Double seconds) {
        return seconds == null ? EMPTY : String.format(Locale.ROOT, "%+.1f", seconds);
    }
}
