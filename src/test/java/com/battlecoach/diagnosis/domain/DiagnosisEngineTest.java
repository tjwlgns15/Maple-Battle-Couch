package com.battlecoach.diagnosis.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;

class DiagnosisEngineTest {

    private static final AnalysisContext CONTEXT =
            AnalysisContext.single(100_000, 1_000, CharacterSpec.of(CooldownStats.of(0, 0, 0, 0), List.of()), List.of(), List.of());

    @Test
    void 영향도_1초_미만은_거르고_영향도_순으로_정렬한다() {
        DiagnosisRule rule = context -> List.of(
                finding("작음", 0.5),
                finding("중간", 2.6),
                finding("큼", 2.9));

        DiagnosisResult result = new DiagnosisEngine(List.of(rule)).diagnose(CONTEXT);

        assertThat(result.findings()).extracting(Finding::skillName).containsExactly("큼", "중간");
    }

    @Test
    void 영향도를_모르는_항목은_참고로_분리한다() {
        DiagnosisRule first = context -> List.of(finding("판데모니움", 2.9));
        DiagnosisRule second = context -> List.of(finding("레디 투 다이", null));

        DiagnosisResult result = new DiagnosisEngine(List.of(first, second)).diagnose(CONTEXT);

        assertThat(result.findings()).extracting(Finding::skillName).containsExactly("판데모니움");
        assertThat(result.notes()).extracting(Finding::skillName).containsExactly("레디 투 다이");
    }

    @Test
    void 같은_스킬의_운용_결과는_영향도가_큰_것만_남기고_분류가_다르면_둘_다_남긴다() {
        DiagnosisRule rule = context -> List.of(
                new Finding(FindingType.MISSED_CAST, "스틱스", "스틱스", 2.6, "놓친 시전"),
                new Finding(FindingType.CAST_COUNT_GAP, "스틱스", "스틱스", 3.1, "시전 수 부족"),
                new Finding(FindingType.MISSING_SKILL, "스틱스", "스틱스", 1.5, "미보유"));

        DiagnosisResult result = new DiagnosisEngine(List.of(rule)).diagnose(CONTEXT);

        assertThat(result.findings()).extracting(Finding::type)
                .containsExactly(FindingType.CAST_COUNT_GAP, FindingType.MISSING_SKILL);
    }

    private static Finding finding(String name, Double impact) {
        return new Finding(FindingType.MISSED_CAST, name, name, impact, name);
    }
}
