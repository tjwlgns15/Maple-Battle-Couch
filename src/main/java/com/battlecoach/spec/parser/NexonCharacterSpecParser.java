package com.battlecoach.spec.parser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.battlecoach.nexon.dto.CharacterInfoSpecResponse;
import com.battlecoach.nexon.dto.CharacterInfoSpecResponse.CharacterSkill;
import com.battlecoach.nexon.dto.CharacterInfoSpecResponse.FinalStat;
import com.battlecoach.spec.domain.CharacterSpec;
import com.battlecoach.spec.domain.CooldownStats;
import com.battlecoach.spec.domain.PowerStats;
import com.battlecoach.spec.domain.SkillSpec;
import com.battlecoach.spec.domain.SkillText;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class NexonCharacterSpecParser implements CharacterSpecParser {

    static final String REDUCTION_SECONDS = "재사용 대기시간 감소 (초)";
    static final String REDUCTION_PERCENT = "재사용 대기시간 감소 (%)";
    static final String RESET_CHANCE = "재사용 대기시간 미적용";
    static final String BUFF_DURATION = "버프 지속시간";
    static final String COMBAT_POWER = "전투력";

    private final JsonMapper jsonMapper;

    @Override
    public CharacterSpec parse(String characterInfoJson) {
        CharacterInfoSpecResponse response = jsonMapper.readValue(characterInfoJson, CharacterInfoSpecResponse.class);
        Map<String, String> finalStats = byName(finalStats(response));
        return CharacterSpec.of(
                toCooldownStats(finalStats),
                PowerStats.of(Math.round(number(finalStats.get(COMBAT_POWER))), hexaLevelSum(response)),
                toSkillSpecs(skills(response)));
    }

    private static Map<String, String> byName(List<FinalStat> finalStats) {
        return finalStats.stream()
                .filter(stat -> stat.statName() != null && stat.statValue() != null)
                .collect(Collectors.toMap(FinalStat::statName, FinalStat::statValue, (first, second) -> first));
    }

    private static CooldownStats toCooldownStats(Map<String, String> byName) {
        return CooldownStats.of(
                number(byName.get(REDUCTION_SECONDS)),
                number(byName.get(REDUCTION_PERCENT)),
                number(byName.get(RESET_CHANCE)),
                number(byName.get(BUFF_DURATION)));
    }

    private static List<SkillSpec> toSkillSpecs(List<CharacterSkill> skills) {
        return skills.stream()
                .filter(skill -> skill.skillName() != null && !skill.skillName().isBlank())
                .map(skill -> SkillSpec.of(
                        skill.skillName().strip(),
                        skill.skillLevel(),
                        SkillText.of(skill.skillEffect(), skill.skillDescription())))
                .toList();
    }

    private static int hexaLevelSum(CharacterInfoSpecResponse response) {
        return Optional.ofNullable(response.hexaMatrixObject())
                .map(CharacterInfoSpecResponse.HexaMatrixObject::hexaCoreObject)
                .map(CharacterInfoSpecResponse.HexaCoreObject::equipment)
                .orElse(List.of())
                .stream()
                .mapToInt(CharacterInfoSpecResponse.HexaCore::level)
                .sum();
    }

    private static List<FinalStat> finalStats(CharacterInfoSpecResponse response) {
        return Optional.ofNullable(response.statObject())
                .map(CharacterInfoSpecResponse.StatObject::basicStatObject)
                .map(CharacterInfoSpecResponse.BasicStatObject::finalStat)
                .orElse(List.of());
    }

    private static List<CharacterSkill> skills(CharacterInfoSpecResponse response) {
        return Optional.ofNullable(response.skillObject())
                .map(CharacterInfoSpecResponse.SkillObject::characterSkill)
                .orElse(List.of());
    }

    /** 스탯이 없거나 숫자가 아니면 0 으로 본다. */
    private static double number(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
