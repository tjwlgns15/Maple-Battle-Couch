package com.battlecoach.replay.application.dto;

import java.util.List;

import com.battlecoach.spec.domain.CooldownStats;

/**
 * 스킬별 계산 쿨과 실측 시전 간격. 쿨 계산이 맞는지 눈으로 검증하고, 이후 진단 규칙의 입력으로 쓴다.
 *
 * @param rows 기본 쿨 내림차순, 쿨을 모르는 스킬은 뒤로
 */
public record CooldownReport(CooldownStats stats, List<Row> rows) {

    /**
     * @param earlyIntervalCount 계산 쿨보다 짧았던 간격 수. 미적용 발동이나 실행 중 쿨 변동(오블리비온 등)의 흔적이다.
     */
    public record Row(
            String skillName,
            Integer level,
            Long baseCooldownMs,
            Long effectiveCooldownMs,
            int castCount,
            Long minIntervalMs,
            Long medianIntervalMs,
            int earlyIntervalCount,
            List<Note> notes
    ) {
    }

    public enum Note {
        NO_SPEC("스킬 정보 없음"),
        NO_COOLDOWN("쿨 표기 없음"),
        NOT_REDUCIBLE("쿨감 적용 안 됨"),
        NOT_RESETTABLE("초기화·미적용 안 됨"),
        REACTIVATION_MERGED("지속 중 다시 누른 입력은 시전에서 뺌");

        private final String label;

        Note(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
