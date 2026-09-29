package com.battlecoach.spec.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.SkillSpec;

import tools.jackson.databind.json.JsonMapper;

/** 칼리 캐릭터 한 명의 연무장 character-info 원문에서 필요한 필드만 남기고 캐릭터명을 가린 샘플로 검증한다. */
class NexonCharacterSpecParserTest {

    private static CharacterSpec spec;

    @BeforeAll
    static void parseSample() throws IOException {
        try (InputStream in = NexonCharacterSpecParserTest.class.getResourceAsStream("/nexon/character-info-kali.json")) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            spec = new NexonCharacterSpecParser(JsonMapper.builder().build()).parse(json);
        }
    }

    @Test
    void 쿨타임_스탯을_읽는다() {
        assertThat(spec.cooldownStats().reductionSeconds()).isEqualTo(5);
        assertThat(spec.cooldownStats().reductionPercent()).isEqualTo(6);
        assertThat(spec.cooldownStats().resetChancePercent()).isEqualTo(27);
        assertThat(spec.cooldownStats().buffDurationPercent()).isEqualTo(76);
    }

    @Test
    void 스킬별_기본_쿨을_읽는다() {
        assertThat(cooldownOf("헥스 : 판데모니움")).isEqualTo(30_000L);
        assertThat(cooldownOf("크레스트 오브 더 솔라")).isEqualTo(240_000L); // "재사용 대기시간 : 240초"
        assertThat(cooldownOf("아츠 : 크레센텀")).isEqualTo(5_000L);        // 다른 스킬 쿨 감소 문구가 앞에 있다
        assertThat(cooldownOf("에르다 노바")).isEqualTo(100_000L);          // Lv30 기준
        assertThat(spec.find("아츠 : 플러리")).get().extracting(SkillSpec::hasCooldown).isEqualTo(false);
    }

    @Test
    void 헥사_강화_항목을_우선한다() {
        SkillSpec deathBlossom = spec.find("데스 블로섬").orElseThrow();

        assertThat(deathBlossom.skillName()).isEqualTo("데스 블로섬 VI");
        assertThat(deathBlossom.level()).isEqualTo(30);
        assertThat(deathBlossom.baseCooldownMs()).isEqualTo(60_000L);
    }

    @Test
    void 쿨감과_초기화_예외_문구를_읽는다() {
        SkillSpec continuousRing = spec.find("컨티뉴어스 링").orElseThrow();
        SkillSpec restraintRing = spec.find("리스트레인트 링").orElseThrow();

        assertThat(continuousRing.cooldownReducible()).isFalse();
        assertThat(restraintRing.cooldownReducible()).isTrue();
        assertThat(restraintRing.cooldownResettable()).isFalse();
        assertThat(spec.find("헥스 : 판데모니움").orElseThrow().cooldownResettable()).isTrue();
    }

    @Test
    void 전투력을_읽고_헥사_매트릭스가_없으면_레벨_합은_0이다() {
        // 샘플에는 hexa_matrix_object 를 남기지 않았다
        assertThat(spec.powerStats().combatPower()).isEqualTo(563_346_261L);
        assertThat(spec.powerStats().hasHexaLevels()).isFalse();
    }

    @Test
    void 강화_코어_레벨을_스킬_레벨과_함께_읽는다() {
        // "헥스 : 판데모니움 강화" Lv20 이 따로 온다
        assertThat(spec.levelOf("헥스 : 판데모니움")).get()
                .satisfies(level -> assertThat(level.enhancementLevel()).isEqualTo(20));
        assertThat(spec.levelOf("크레스트 오브 더 솔라")).get()
                .satisfies(level -> assertThat(level.enhancementLevel()).isNull());
    }

    @Test
    void 캐릭터_스킬_목록에_없는_스킬은_찾지_못한다() {
        assertThat(spec.find("소울 컨트랙트")).isEmpty();
    }

    private static Long cooldownOf(String baseName) {
        return spec.find(baseName).orElseThrow().baseCooldownMs();
    }
}
