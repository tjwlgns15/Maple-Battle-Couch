package com.battlecoach.ranker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.battlecoach.ranker.domain.RankerSample;

public interface RankerSampleRepository extends JpaRepository<RankerSample, String> {

    List<RankerSample> findByCharacterClassAndPeriodNo(String characterClass, int periodNo);
}
