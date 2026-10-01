package com.battlecoach.replay.application.dto;

import java.util.ArrayList;
import java.util.List;

import com.battlecoach.spec.domain.CooldownSources;
import com.battlecoach.spec.domain.CooldownStats;
import com.battlecoach.spec.domain.StatSource;

/** 쿨감·미적용·버프 지속시간 합계와 출처. 상세 화면 상단과 비교 화면에 보여준다. */
public record CooldownStatsView(List<Item> items) {

    /** 출처 합과 합계의 차이가 이보다 작으면 같다고 본다 */
    private static final double EPSILON = 0.01;

    public CooldownStatsView {
        items = List.copyOf(items);
    }

    public static CooldownStatsView of(CooldownStats stats, CooldownSources sources) {
        return new CooldownStatsView(List.of(
                new Item("쿨타임 감소", "-", "초", stats.reductionSeconds(), sources.reductionSeconds(), true),
                new Item("쿨타임 감소", "-", "%", stats.reductionPercent(), sources.reductionPercent(), true),
                new Item("재사용 대기시간 미적용", "", "%", stats.resetChancePercent(), sources.resetChance(), true),
                new Item("버프 지속시간", "+", "%", stats.buffDurationPercent(), List.of(), false)));
    }

    /**
     * @param sign           화면에 값 앞에 붙이는 부호 (감소는 "-", 증가는 "+")
     * @param total          final_stat 합계(게임 표기)
     * @param sources        찾은 출처. 버프 지속시간은 출처를 모아 합을 맞추지 못해 비어 있다
     * @param sourcesTracked 출처를 찾는 항목인지. false 면 화면에 "합계만"으로 보여준다
     */
    public record Item(String name, String sign, String unit, double total, List<StatSource> sources, boolean sourcesTracked) {

        public Item {
            sources = List.copyOf(sources);
        }

        public double sourceSum() {
            return sources.stream().mapToDouble(StatSource::value).sum();
        }

        /** 출처 합이 합계보다 작으면 모자란 몫을 "기타"로 붙인다. 직업 패시브 등 아직 찾지 못한 출처다. */
        public List<StatSource> displaySources() {
            double missing = total - sourceSum();
            if (!sourcesTracked || missing < EPSILON) {
                return sources;
            }
            List<StatSource> withOther = new ArrayList<>(sources);
            withOther.add(StatSource.of("기타", missing));
            return withOther;
        }

        /** 게임 표기가 출처 합을 내림한 값인지 (미적용 27.5% → 27%) */
        public boolean roundedDown() {
            double sum = sourceSum();
            return sum - total > EPSILON && Math.floor(sum) == total;
        }

        public boolean differsFrom(Item other) {
            return Math.abs(total - other.total) > EPSILON;
        }
    }
}
