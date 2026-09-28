package com.battlecoach.diagnosis.sequence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class NeedlemanWunschAlignerTest {

    private final NeedlemanWunschAligner aligner = new NeedlemanWunschAligner();

    @Test
    void 같은_순서는_모두_일치한다() {
        List<AlignedPair> pairs = aligner.align(List.of("A", "B", "C"), List.of("A", "B", "C"));

        assertThat(pairs).extracting(AlignedPair::status).containsOnly(AlignedPair.Status.MATCH);
    }

    @Test
    void 한쪽에만_있는_스킬은_간격으로_맞춘다() {
        // 연자히에는 플레게톤이 없다
        List<AlignedPair> pairs = aligner.align(
                List.of("데스 블로섬", "레이스 오브 갓", "오블리비온"),
                List.of("데스 블로섬", "플레게톤", "레이스 오브 갓", "오블리비온"));

        assertThat(pairs).containsExactly(
                new AlignedPair("데스 블로섬", "데스 블로섬"),
                new AlignedPair(null, "플레게톤"),
                new AlignedPair("레이스 오브 갓", "레이스 오브 갓"),
                new AlignedPair("오블리비온", "오블리비온"));
    }

    @Test
    void 순서가_바뀐_스킬은_서로_다른_스킬과_짝짓지_않고_양쪽에_따로_드러난다() {
        List<AlignedPair> pairs = aligner.align(
                List.of("레디 투 다이", "보이드 버스트", "스틱스"),
                List.of("레디 투 다이", "스틱스", "보이드 버스트"));

        assertThat(pairs).extracting(AlignedPair::status).doesNotContain(AlignedPair.Status.MISMATCH);
        assertThat(pairs).filteredOn(pair -> pair.status() == AlignedPair.Status.MATCH).hasSize(2);
        assertThat(pairs).hasSize(4);
    }

    @Test
    void 빈_순서도_처리한다() {
        assertThat(aligner.align(List.of(), List.of("A"))).containsExactly(new AlignedPair(null, "A"));
        assertThat(aligner.align(List.of(), List.of())).isEmpty();
    }
}
