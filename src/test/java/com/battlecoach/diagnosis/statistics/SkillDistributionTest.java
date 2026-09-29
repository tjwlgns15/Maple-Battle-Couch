package com.battlecoach.diagnosis.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.statistics.SkillDistribution.SecondsBasis;
import com.battlecoach.diagnosis.statistics.SkillDistribution.SecondsSample;
import com.battlecoach.spec.domain.SkillLevel;

class SkillDistributionTest {

    private static final SkillLevel MAX = new SkillLevel(30, 30);
    private static final SkillLevel LOW = new SkillLevel(30, 10);

    @Test
    void 레벨이_같은_랭커가_5명_이상이면_그들만으로_비교한다() {
        List<SecondsSample> samples = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            samples.add(new SecondsSample(20 + i, MAX, 1.0));
        }
        samples.add(new SecondsSample(5, LOW, 1.0));
        SkillDistribution distribution = distribution(samples);

        SecondsBasis basis = distribution.secondsFor(MAX).orElseThrow();

        assertThat(basis.levelMatched()).isTrue();
        assertThat(basis.sampleCount()).isEqualTo(5);
        assertThat(basis.quartiles().p50()).isEqualTo(22.0);
    }

    @Test
    void 같은_레벨이_부족하거나_내_레벨을_모르면_전체_분포로_비교한다() {
        SkillDistribution distribution = distribution(List.of(
                new SecondsSample(20, MAX, 1.0), new SecondsSample(5, LOW, 1.0)));

        assertThat(distribution.secondsFor(LOW).orElseThrow().levelMatched()).isFalse();
        assertThat(distribution.secondsFor(null).orElseThrow().sampleCount()).isEqualTo(2);
    }

    private static SkillDistribution distribution(List<SecondsSample> samples) {
        Quartiles all = Quartiles.of(samples.stream().map(SecondsSample::seconds).toList());
        return new SkillDistribution("헥스 : 판데모니움", "헥스 : 판데모니움", samples.size(), 1.0, all, all, samples);
    }
}
