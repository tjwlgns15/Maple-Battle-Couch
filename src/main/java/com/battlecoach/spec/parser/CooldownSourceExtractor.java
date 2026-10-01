package com.battlecoach.spec.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.battlecoach.nexon.dto.CharacterInfoSpecResponse;
import com.battlecoach.nexon.dto.CharacterInfoSpecResponse.AbilityInfo;
import com.battlecoach.nexon.dto.CharacterInfoSpecResponse.ItemEquipment;
import com.battlecoach.nexon.dto.CharacterInfoSpecResponse.UnionArtifactEffect;
import com.battlecoach.spec.domain.CooldownSources;
import com.battlecoach.spec.domain.StatSource;

/**
 * character-info 에서 쿨감·미적용 출처를 뽑는다. 문구는 저장된 기록 36건에서 확인한 형식이다.
 * <ul>
 *   <li>장비 잠재: "스킬 재사용 대기시간 -2초"</li>
 *   <li>유니온 공격대원: "스킬 재사용 대기시간 6% 감소"</li>
 *   <li>어빌리티: "스킬 사용 시 20% 확률로 재사용 대기시간이 미적용"</li>
 *   <li>유니온 아티팩트: "재사용 대기시간 미적용 확률 7.50% 증가"</li>
 * </ul>
 */
final class CooldownSourceExtractor {

    private static final String NUMBER = "(\\d+(?:\\.\\d+)?)";
    private static final Pattern ITEM_SECONDS = Pattern.compile("재사용 대기시간 -" + NUMBER + "초");
    private static final Pattern RAIDER_PERCENT = Pattern.compile("재사용 대기시간 " + NUMBER + "% 감소");
    private static final Pattern ABILITY_RESET = Pattern.compile(NUMBER + "% 확률로 재사용 대기시간이 미적용");
    private static final Pattern ARTIFACT_RESET = Pattern.compile("재사용 대기시간 미적용 확률 " + NUMBER + "% 증가");

    private CooldownSourceExtractor() {
    }

    static CooldownSources extract(CharacterInfoSpecResponse response) {
        return CooldownSources.of(itemSeconds(response), raiderPercent(response), resetChance(response));
    }

    /** 장비 부위마다 잠재·에디셔널 잠재를 따로 더한다. 0 인 항목은 뺀다. */
    private static List<StatSource> itemSeconds(CharacterInfoSpecResponse response) {
        List<StatSource> sources = new ArrayList<>();
        for (ItemEquipment item : items(response)) {
            String slot = item.slot() == null ? "장비" : item.slot();
            addIfPositive(sources, slot + " 잠재능력", sum(item.potentialOptions().stream(), ITEM_SECONDS));
            addIfPositive(sources, slot + " 에디셔널", sum(item.additionalPotentialOptions().stream(), ITEM_SECONDS));
        }
        return sources;
    }

    private static List<StatSource> raiderPercent(CharacterInfoSpecResponse response) {
        List<StatSource> sources = new ArrayList<>();
        Stream<String> stats = Optional.ofNullable(response.unionRaiderObject())
                .map(CharacterInfoSpecResponse.UnionRaiderObject::unionRaiderStat)
                .orElse(List.of())
                .stream();
        addIfPositive(sources, "유니온 공격대원", sum(stats, RAIDER_PERCENT));
        return sources;
    }

    private static List<StatSource> resetChance(CharacterInfoSpecResponse response) {
        List<StatSource> sources = new ArrayList<>();
        Stream<String> abilities = Optional.ofNullable(response.abilityObject())
                .map(CharacterInfoSpecResponse.AbilityObject::abilityInfo)
                .orElse(List.<AbilityInfo>of())
                .stream()
                .map(AbilityInfo::value);
        addIfPositive(sources, "어빌리티", sum(abilities, ABILITY_RESET));
        Stream<String> artifacts = Optional.ofNullable(response.unionArtifactObject())
                .map(CharacterInfoSpecResponse.UnionArtifactObject::effects)
                .orElse(List.<UnionArtifactEffect>of())
                .stream()
                .map(UnionArtifactEffect::name);
        addIfPositive(sources, "유니온 아티팩트", sum(artifacts, ARTIFACT_RESET));
        return sources;
    }

    private static List<ItemEquipment> items(CharacterInfoSpecResponse response) {
        return Optional.ofNullable(response.itemObject())
                .map(CharacterInfoSpecResponse.ItemObject::itemEquipmentObject)
                .map(CharacterInfoSpecResponse.ItemEquipmentObject::itemEquipment)
                .orElse(List.of());
    }

    private static double sum(Stream<String> texts, Pattern pattern) {
        return texts.filter(text -> text != null)
                .map(pattern::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .mapToDouble(Double::parseDouble)
                .sum();
    }

    private static void addIfPositive(List<StatSource> sources, String label, double value) {
        if (value > 0) {
            sources.add(StatSource.of(label, value));
        }
    }
}
