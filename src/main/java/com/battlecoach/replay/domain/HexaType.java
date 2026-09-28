package com.battlecoach.replay.domain;

import java.util.Arrays;

/** skill-timeline 의 hexa_skill_specificity_flag */
public enum HexaType {

    NORMAL("0"),
    ORIGIN("1"),
    ASCENT("2"),
    UNKNOWN(null);

    private final String flag;

    HexaType(String flag) {
        this.flag = flag;
    }

    public static HexaType fromFlag(String flag) {
        return Arrays.stream(values())
                .filter(type -> type.flag != null && type.flag.equals(flag))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
