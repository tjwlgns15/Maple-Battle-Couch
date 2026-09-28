package com.battlecoach.ranker.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.battlecoach.ranker.domain.RankerProbe;

public interface RankerProbeRepository extends JpaRepository<RankerProbe, Long> {

    boolean existsByJobClassAndCharacterNameAndPeriodNo(String jobClass, String characterName, int periodNo);
}
