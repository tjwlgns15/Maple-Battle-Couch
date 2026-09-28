package com.battlecoach.replay.application.dto;

import java.util.List;

import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.DiagnosisResult;

public record ReplayAnalysis(
        DiagnosisResult diagnosis,
        List<BurstWindow> bursts,
        CooldownReport cooldowns,
        RankerStanding rankerStanding
) {
}
