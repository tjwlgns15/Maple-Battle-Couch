package com.battlecoach.diagnosis.domain;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** 모든 규칙을 돌려 영향도가 의미 있는 것만 영향도 순으로 남긴다. */
@Component
@RequiredArgsConstructor
public class DiagnosisEngine {

    /** 이보다 작은 손해는 측정 오차와 구분하기 어려워 보여주지 않는다. (에르다 노바 1회 누락 ≈ 0.02초) */
    public static final double MIN_IMPACT_SECONDS = 1.0;

    private static final Comparator<Finding> BY_IMPACT = Comparator.comparingDouble(Finding::impactSeconds);

    private final List<DiagnosisRule> rules;

    public DiagnosisResult diagnose(AnalysisContext context) {
        List<Finding> all = rules.stream()
                .flatMap(rule -> rule.evaluate(context).stream())
                .toList();

        List<Finding> measured = deduplicate(all.stream()
                .filter(Finding::isMeasured)
                .filter(finding -> finding.impactSeconds() >= MIN_IMPACT_SECONDS)
                .toList());
        List<Finding> notes = all.stream()
                .filter(finding -> !finding.isMeasured())
                .toList();
        return new DiagnosisResult(measured, notes);
    }

    /**
     * 같은 스킬·같은 분류의 결과가 여러 규칙에서 나오면 영향도가 가장 큰 것만 남긴다.
     * 놓친 시전과 시전 수 부족은 같은 손해를 다른 기준으로 잰 것이라 더하면 두 번 센다.
     */
    private static List<Finding> deduplicate(List<Finding> findings) {
        Map<String, Finding> bySkillAndCategory = new LinkedHashMap<>();
        findings.forEach(finding -> bySkillAndCategory.merge(
                finding.skillBaseName() + "|" + finding.type().category(),
                finding,
                BinaryOperator.maxBy(BY_IMPACT)));
        return bySkillAndCategory.values().stream()
                .sorted(BY_IMPACT.reversed())
                .toList();
    }
}
