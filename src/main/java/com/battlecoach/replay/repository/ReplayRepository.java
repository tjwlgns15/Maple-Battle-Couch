package com.battlecoach.replay.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.battlecoach.replay.domain.Replay;

public interface ReplayRepository extends JpaRepository<Replay, String> {

    /** 같은 직업·기간으로 저장된 리플레이. 기간은 replay_period 에만 있다. */
    @Query("""
            select r.replayId from Replay r, ReplayPeriod p
            where p.replayId = r.replayId and p.periodNo = :periodNo
              and r.characterProfile.characterClass = :characterClass
            """)
    List<String> findReplayIds(@Param("characterClass") String characterClass, @Param("periodNo") int periodNo);

    @Query("""
            select new com.battlecoach.replay.repository.PeriodCount(p.periodNo, count(r))
            from Replay r, ReplayPeriod p where p.replayId = r.replayId
            group by p.periodNo order by p.periodNo desc
            """)
    List<PeriodCount> countByPeriod();

    @Query("""
            select new com.battlecoach.replay.repository.ClassCount(r.characterProfile.characterClass, count(r))
            from Replay r, ReplayPeriod p where p.replayId = r.replayId and p.periodNo = :periodNo
            group by r.characterProfile.characterClass
            order by count(r) desc, r.characterProfile.characterClass
            """)
    List<ClassCount> countByClass(@Param("periodNo") int periodNo);

    /** 마지막으로 API 에서 받은(또는 갱신한) 시각이 before 보다 오래된 리플레이. 오래된 것부터 */
    @Query("""
            select r.replayId from Replay r
            where coalesce(r.refreshedAt, r.fetchedAt) < :before
            order by coalesce(r.refreshedAt, r.fetchedAt)
            """)
    List<String> findStale(@Param("before") LocalDateTime before, Pageable pageable);
}
