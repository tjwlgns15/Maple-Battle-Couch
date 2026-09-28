package com.battlecoach.replay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "replay_skill_stat", indexes = @Index(name = "idx_skill_stat_base_name", columnList = "base_name"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplaySkillStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "replay_id", nullable = false)
    private Replay replay;

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName;

    @Column(name = "base_name", nullable = false, length = 100)
    private String baseName;

    @Embedded
    private SkillDamage damage;

    private ReplaySkillStat(Replay replay, SkillName skillName, SkillDamage damage) {
        this.replay = replay;
        this.skillName = skillName.value();
        this.baseName = skillName.baseName();
        this.damage = damage;
    }

    static ReplaySkillStat create(Replay replay, SkillName skillName, SkillDamage damage) {
        return new ReplaySkillStat(replay, skillName, damage);
    }
}
