package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.Contest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestRepository extends JpaRepository<Contest, UUID> {

    boolean existsBySlug(String slug);

    @Query(
            "SELECT COUNT(p) > 0 FROM ContestProblem p"
                    + " WHERE p.problemId = :problemId AND p.contest.endAt > :now")
    boolean isProblemInUnfinishedContest(
            @Param("problemId") UUID problemId, @Param("now") Instant now);

    @Query(
            "SELECT COUNT(r) > 0 FROM ContestRegistration r WHERE r.userId = :userId"
                    + " AND r.contest.rated = true AND r.contest.startAt <= :now"
                    + " AND r.contest.endAt > :now")
    boolean isUserInRunningRatedContest(@Param("userId") UUID userId, @Param("now") Instant now);

    Optional<Contest> findBySlug(String slug);

    Page<Contest> findByStartAtAfterOrderByStartAtAsc(Instant now, Pageable pageable);

    @Query(
            "SELECT c FROM Contest c WHERE c.startAt <= :now AND c.endAt > :now"
                    + " ORDER BY c.endAt ASC")
    Page<Contest> findRunningAt(Instant now, Pageable pageable);

    Page<Contest> findByEndAtLessThanEqualOrderByEndAtDesc(Instant now, Pageable pageable);

    Page<Contest> findAllByOrderByStartAtDesc(Pageable pageable);

    @Query(
            "SELECT c FROM Contest c WHERE (c.startedAnnouncedAt IS NULL AND c.startAt <= :now)"
                    + " OR (c.finishedAnnouncedAt IS NULL AND c.endAt <= :now) ORDER BY c.startAt")
    List<Contest> findDueForAnnouncement(Instant now);

    @Query(
            "SELECT new com.sqljudge.contest.repository.StatementWarmupTarget(c.id, p.problemId)"
                    + " FROM Contest c JOIN c.problems p WHERE c.statementsWarmedAt IS NULL"
                    + " AND c.startAt <= :cutoff AND c.endAt > :now ORDER BY c.startAt, p.position")
    List<StatementWarmupTarget> findDueForStatementWarmup(Instant now, Instant cutoff);

    @Query("SELECT c FROM Contest c WHERE c.finalizedAt IS NULL AND c.endAt <= :cutoff")
    List<Contest> findDueForFinalization(Instant cutoff);

    @Query(
            "SELECT c FROM Contest c WHERE c.rated = true AND c.finalizedAt IS NOT NULL"
                    + " AND c.ratingAppliedAt IS NULL")
    List<Contest> findDueForRating();
}
