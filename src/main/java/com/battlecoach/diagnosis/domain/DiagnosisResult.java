package com.battlecoach.diagnosis.domain;

import java.util.List;

/**
 * @param findings 영향도 {@link DiagnosisEngine#MIN_IMPACT_SECONDS} 이상, 영향도 내림차순
 * @param notes    영향도를 계산할 수 없는 참고 사항 (버프 누락, 진단 제외 스킬 등)
 */
public record DiagnosisResult(List<Finding> findings, List<Finding> notes) {

    public DiagnosisResult {
        findings = List.copyOf(findings);
        notes = List.copyOf(notes);
    }
}
