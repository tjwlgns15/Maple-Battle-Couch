package com.battlecoach.replay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.battlecoach.replay.domain.ReplayRawData;

public interface ReplayRawDataRepository extends JpaRepository<ReplayRawData, String> {
}
