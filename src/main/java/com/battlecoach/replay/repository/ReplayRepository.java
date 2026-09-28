package com.battlecoach.replay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.battlecoach.replay.domain.Replay;

public interface ReplayRepository extends JpaRepository<Replay, String> {
}
