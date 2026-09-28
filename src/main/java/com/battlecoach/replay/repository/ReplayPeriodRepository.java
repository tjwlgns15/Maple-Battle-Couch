package com.battlecoach.replay.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.battlecoach.replay.domain.ReplayPeriod;

public interface ReplayPeriodRepository extends JpaRepository<ReplayPeriod, String> {

    /** 지금까지 본 가장 최근 기간 */
    @Query("select max(p.periodNo) from ReplayPeriod p")
    Optional<Integer> findLatestPeriodNo();
}
