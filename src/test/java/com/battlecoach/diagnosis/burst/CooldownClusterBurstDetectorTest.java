package com.battlecoach.diagnosis.burst;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.battlecoach.diagnosis.domain.BurstWindow;
import com.battlecoach.diagnosis.domain.SkillUsage;

class CooldownClusterBurstDetectorTest {

    private final CooldownClusterBurstDetector detector = new CooldownClusterBurstDetector();

    @Test
    void 쿨이_긴_스킬_3개가_3초_안에_몰리면_극딜로_보고_지속시간_중앙값만큼_잡는다() {
        List<BurstWindow> windows = detector.detect(List.of(
                skill("레이스 오브 갓", 108_000L, 60_000L, 2_000, 112_000),
                skill("오블리비온", 108_000L, 30_000L, 2_500, 112_500),
                skill("리스트레인트 링", 108_000L, 15_000L, 3_000, 113_000)));

        assertThat(windows).containsExactly(
                new BurstWindow(2_000, 32_000),
                new BurstWindow(112_000, 142_000));
    }

    @Test
    void 쿨이_짧은_스킬이나_흩어진_시전은_극딜이_아니다() {
        List<BurstWindow> windows = detector.detect(List.of(
                skill("판데모니움", 23_000L, 3_000L, 1_000),
                skill("데스 블로섬", 51_000L, 30_000L, 1_500),
                skill("레이스 오브 갓", 108_000L, 60_000L, 2_000),
                skill("오블리비온", 108_000L, 30_000L, 10_000)));

        assertThat(windows).isEmpty();
    }

    @Test
    void 지속시간을_모르면_구간을_만들지_않는다() {
        List<BurstWindow> windows = detector.detect(List.of(
                skill("A", 120_000L, null, 1_000),
                skill("B", 120_000L, null, 1_500),
                skill("C", 120_000L, null, 2_000)));

        assertThat(windows).isEmpty();
    }

    @Test
    void 지속시간이_쿨보다_긴_상시_버프는_구간_길이에서_뺀다() {
        // 쓸만한 샤프 아이즈(쿨 180초, 지속 270초 × 버프 지속 증가)가 극딜 때 함께 나가도 극딜 길이를 늘리지 않는다
        List<BurstWindow> windows = detector.detect(List.of(
                skill("그란디스", 120_000L, 40_000L, 1_000),
                skill("오블리비온", 120_000L, 30_000L, 1_500),
                skill("쓸만한 샤프 아이즈", 180_000L, 480_000L, 2_000),
                skill("쓸만한 하이퍼 바디", 180_000L, 480_000L, 2_500)));

        assertThat(windows).containsExactly(new BurstWindow(1_000, 41_000));
    }

    @Test
    void 겹치는_구간은_합친다() {
        List<BurstWindow> windows = detector.detect(List.of(
                skill("A", 120_000L, 30_000L, 0, 10_000),
                skill("B", 120_000L, 30_000L, 1_000, 11_000),
                skill("C", 120_000L, 30_000L, 2_000, 12_000)));

        assertThat(windows).containsExactly(new BurstWindow(0, 40_000));
    }

    private static SkillUsage skill(String name, Long cooldownMs, Long durationMs, long... castTimes) {
        return new SkillUsage(name, name, Arrays.stream(castTimes).boxed().toList(), null, cooldownMs, durationMs);
    }
}
