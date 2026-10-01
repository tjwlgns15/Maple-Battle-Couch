package com.battlecoach.replay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.battlecoach.replay.domain.ReplayPeriod;

public interface ReplayPeriodRepository extends JpaRepository<ReplayPeriod, String> {
}
