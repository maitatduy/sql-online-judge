package com.sqljudge.contest.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "contests",
        indexes = {
            @Index(name = "uk_contests_slug", columnList = "slug", unique = true),
            @Index(name = "idx_contests_start_at", columnList = "start_at"),
            @Index(name = "idx_contests_end_at", columnList = "end_at")
        })
@Getter
@NoArgsConstructor
public class Contest extends BaseEntity {

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "freeze_minutes", nullable = false)
    private int freezeMinutes;

    @Column(name = "penalty_minutes", nullable = false)
    private int penaltyMinutes;

    @Column(name = "is_rated", nullable = false)
    private boolean rated;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static Contest schedule(ContestSchedule schedule) {
        Contest contest = new Contest();
        contest.slug = schedule.slug();
        contest.title = schedule.title();
        contest.description = schedule.description();
        contest.startAt = schedule.startAt();
        contest.durationMinutes = schedule.durationMinutes();
        contest.endAt = schedule.startAt().plus(Duration.ofMinutes(schedule.durationMinutes()));
        contest.freezeMinutes = schedule.freezeMinutes();
        contest.penaltyMinutes = schedule.penaltyMinutes();
        contest.rated = schedule.rated();
        return contest;
    }

    public ContestPhase phaseAt(Instant now) {
        if (now.isBefore(startAt)) {
            return ContestPhase.UPCOMING;
        }
        return now.isBefore(endAt) ? ContestPhase.RUNNING : ContestPhase.FINISHED;
    }
}
