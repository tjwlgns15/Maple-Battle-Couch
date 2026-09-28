package com.battlecoach.replay.application.dto;

import java.time.LocalDate;

/** 연무장 기록 목록의 한 줄. 기간(period_no)마다 최대 1건이다. */
public record ReplayListItem(int periodNo, LocalDate registerDate, String replayId) {

    public static ReplayListItem of(int periodNo, LocalDate registerDate, String replayId) {
        return new ReplayListItem(periodNo, registerDate, replayId);
    }
}
