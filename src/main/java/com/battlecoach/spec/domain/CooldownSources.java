package com.battlecoach.spec.domain;

import java.util.List;

/**
 * {@link CooldownStats} 합계가 어디서 왔는지. 화면 표시용이고 계산에는 쓰지 않는다.
 * 저장된 기록 36건(9개 직업)에서 합이 final_stat 과 일치한 출처만 담는다(미적용은 합을 내림한 값이 final_stat).
 * 버프 지속시간은 출처가 유니온·아티팩트·직업 패시브 등에 흩어져 있어 합을 맞추지 못해 담지 않는다.
 *
 * @param reductionSeconds 장비 잠재·에디셔널 잠재의 "스킬 재사용 대기시간 -N초"
 * @param reductionPercent 유니온 공격대원 효과의 "스킬 재사용 대기시간 N% 감소"
 * @param resetChance      어빌리티와 유니온 아티팩트의 재사용 대기시간 미적용 확률
 */
public record CooldownSources(
        List<StatSource> reductionSeconds,
        List<StatSource> reductionPercent,
        List<StatSource> resetChance
) {

    public CooldownSources {
        reductionSeconds = List.copyOf(reductionSeconds);
        reductionPercent = List.copyOf(reductionPercent);
        resetChance = List.copyOf(resetChance);
    }

    public static CooldownSources of(List<StatSource> reductionSeconds, List<StatSource> reductionPercent,
                                     List<StatSource> resetChance) {
        return new CooldownSources(reductionSeconds, reductionPercent, resetChance);
    }

    public static CooldownSources none() {
        return new CooldownSources(List.of(), List.of(), List.of());
    }
}
