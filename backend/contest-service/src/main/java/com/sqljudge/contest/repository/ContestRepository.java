package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.Contest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContestRepository extends JpaRepository<Contest, UUID> {

    boolean existsBySlug(String slug);

    Optional<Contest> findBySlug(String slug);

    Page<Contest> findByStartAtAfterOrderByStartAtAsc(Instant now, Pageable pageable);

    @Query(
            "SELECT c FROM Contest c WHERE c.startAt <= :now AND c.endAt > :now"
                    + " ORDER BY c.endAt ASC")
    Page<Contest> findRunningAt(Instant now, Pageable pageable);

    Page<Contest> findByEndAtLessThanEqualOrderByEndAtDesc(Instant now, Pageable pageable);

    Page<Contest> findAllByOrderByStartAtDesc(Pageable pageable);
}
