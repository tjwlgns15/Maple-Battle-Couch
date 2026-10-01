package com.battlecoach.spec.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.battlecoach.spec.domain.CooldownSources;
import com.battlecoach.spec.domain.StatSource;

import tools.jackson.databind.json.JsonMapper;

/** 문구 형식은 저장된 character-info 원문에서 가져왔다. */
class CooldownSourceExtractorTest {

    private static final String JSON = """
            {
              "stat_object": {"basic_stat_object": {"final_stat": [
                {"stat_name": "재사용 대기시간 감소 (초)", "stat_value": "5"},
                {"stat_name": "재사용 대기시간 감소 (%)", "stat_value": "6"},
                {"stat_name": "재사용 대기시간 미적용", "stat_value": "27"}
              ]}},
              "item_object": {"item_equipment_object": {"item_equipment": [
                {"item_equipment_slot": "모자",
                 "potential_option_1": "스킬 재사용 대기시간 -2초",
                 "potential_option_2": "스킬 재사용 대기시간 -2초",
                 "potential_option_3": "올스탯 +7%",
                 "additional_potential_option_1": "스킬 재사용 대기시간 -1초",
                 "additional_potential_option_2": null},
                {"item_equipment_slot": "상의", "potential_option_1": "STR +12%"}
              ]}},
              "union_raider_object": {"union_raider_stat": ["버프 지속시간 25% 증가", "스킬 재사용 대기시간 6% 감소"]},
              "ability_object": {"ability_info": [
                {"ability_value": "스킬 사용 시 20% 확률로 재사용 대기시간이 미적용"},
                {"ability_value": "보스 몬스터 공격 시 데미지 10% 증가"}
              ]},
              "union_artifact_object": {"union_artifact_effect": [
                {"name": "버프 지속시간 20% 증가"},
                {"name": "재사용 대기시간 미적용 확률 7.50% 증가"}
              ]}
            }
            """;

    private final CooldownSources sources =
            new NexonCharacterSpecParser(JsonMapper.builder().build()).parse(JSON).cooldownSources();

    @Test
    void 장비_잠재와_에디셔널을_부위별로_따로_더한다() {
        assertThat(sources.reductionSeconds()).containsExactly(
                StatSource.of("모자 잠재능력", 4), StatSource.of("모자 에디셔널", 1));
    }

    @Test
    void 유니온_공격대원_쿨감_퍼센트를_읽는다() {
        assertThat(sources.reductionPercent()).containsExactly(StatSource.of("유니온 공격대원", 6));
    }

    @Test
    void 미적용은_어빌리티와_유니온_아티팩트에서_읽는다() {
        assertThat(sources.resetChance()).containsExactly(
                StatSource.of("어빌리티", 20), StatSource.of("유니온 아티팩트", 7.5));
    }

    @Test
    void 출처_객체가_없으면_비어_있다() {
        CooldownSources empty = new NexonCharacterSpecParser(JsonMapper.builder().build()).parse("{}").cooldownSources();

        assertThat(empty).isEqualTo(CooldownSources.none());
    }
}
