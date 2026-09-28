package com.battlecoach.ranker.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 랭커 한 명을 특정 기간 기준으로 확인한 결과. 기록이 없던 캐릭터도 남겨서 다시 수집할 때 API 를 부르지 않는다.
 * (개발 키는 하루 1,000건이라 랭커 한 명 확인에 드는 2건도 아껴야 한다)
 */
@Entity
@Table(name = "ranker_probe", uniqueConstraints = @UniqueConstraint(
        name = "uk_ranker_probe", columnNames = {"job_class", "character_name", "period_no"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankerProbe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 랭킹 조회에 쓴 직업 파라미터. 예: "칼리-전체전직" */
    @Column(name = "job_class", nullable = false, length = 50)
    private String jobClass;

    @Column(name = "character_name", nullable = false, length = 30)
    private String characterName;

    @Column(name = "period_no", nullable = false)
    private int periodNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private ProbeResult result;

    @Column(name = "probed_at", nullable = false)
    private LocalDateTime probedAt;

    private RankerProbe(String jobClass, String characterName, int periodNo, ProbeResult result, LocalDateTime probedAt) {
        this.jobClass = jobClass;
        this.characterName = characterName;
        this.periodNo = periodNo;
        this.result = result;
        this.probedAt = probedAt;
    }

    public static RankerProbe of(String jobClass, String characterName, int periodNo, ProbeResult result,
                                 LocalDateTime probedAt) {
        return new RankerProbe(jobClass, characterName, periodNo, result, probedAt);
    }
}
