package com.battlecoach.replay.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SequenceSegmentTest {

    private Replay replay;
    private int order;

    @BeforeEach
    void setUp() {
        replay = Replay.create(
                "r1",
                CharacterProfile.of("테스트", "칼리", 285),
                BattleSummary.of(LocalDate.of(2026, 8, 6), 360_000, 1, 1, "0", 0),
                LocalDateTime.of(2026, 8, 6, 0, 0));
        order = 0;
    }

    @Test
    void 기록_순서가_역전돼도_시작과_끝은_최소_최대_시각이다() {
        cast(10_005, "판데모니움", "극", "1");
        cast(10_000, "데스 블로섬", "극", "1");
        cast(10_400, "헥스 : 샌드스톰", "극", "1");

        assertThat(replay.getSequenceSegments())
                .extracting(SequenceSegment::sequenceKey, SequenceSegment::startMs, SequenceSegment::endMs,
                        SequenceSegment::castCount)
                .containsExactly(tuple("1", 10_000L, 10_400L, 3));
    }

    @Test
    void 사이에_끼어든_일반_시전은_구간을_끊지_않는다() {
        cast(10_000, "판데모니움", "극", "1");
        cast(10_200, "아츠 : 플러리", null, null);
        cast(18_000, "보이드 버스트", "극", "1");

        assertThat(replay.getSequenceSegments()).singleElement()
                .satisfies(segment -> {
                    assertThat(segment.startMs()).isEqualTo(10_000L);
                    assertThat(segment.endMs()).isEqualTo(18_000L);
                    assertThat(segment.castCount()).isEqualTo(2);
                });
    }

    @Test
    void 같은_키라도_간격이_크면_다른_실행으로_나눈다() {
        cast(10_000, "판데모니움", "극", "1");
        cast(10_000 + SequenceSegment.MAX_GAP_MS + 1, "판데모니움", "극", "1");

        assertThat(replay.getSequenceSegments())
                .extracting(SequenceSegment::startMs)
                .containsExactly(10_000L, 10_000L + SequenceSegment.MAX_GAP_MS + 1);
    }

    @Test
    void 키가_다른_시퀀스는_따로_묶고_시작_시각_순으로_정렬한다() {
        cast(5_000, "스틱스", "준극", "2");
        cast(6_000, "판데모니움", "극", "1");
        cast(5_500, "레디 투 다이", "준극", "2");

        List<SequenceSegment> segments = replay.getSequenceSegments();

        assertThat(segments)
                .extracting(SequenceSegment::sequenceName, SequenceSegment::startMs, SequenceSegment::castCount)
                .containsExactly(tuple("준극", 5_000L, 2), tuple("극", 6_000L, 1));
    }

    @Test
    void 시퀀스가_없으면_빈_목록이다() {
        cast(1_000, "아츠 : 플러리", null, null);

        assertThat(replay.getSequenceSegments()).isEmpty();
    }

    private void cast(long elapseMs, String skillName, String sequenceName, String sequenceKey) {
        replay.addCast(order++, elapseMs, SkillName.of(skillName), HexaType.NORMAL, sequenceName, sequenceKey);
    }
}
