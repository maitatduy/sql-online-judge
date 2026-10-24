package com.sqljudge.auth.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_restrictions",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_user_restrictions_user_id", columnNames = "user_id"))
@Getter
@NoArgsConstructor
public class UserRestriction extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "ai_daily_quota")
    private Integer aiDailyQuota;

    @Column(name = "is_banned_from_rated_contests", nullable = false)
    private boolean isBannedFromRatedContests;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public static UserRestriction defaultsFor(User user) {
        UserRestriction restriction = new UserRestriction();
        restriction.userId = user.getId();
        return restriction;
    }

    public int effectiveAiDailyQuota(int defaultAiDailyQuota) {
        return aiDailyQuota == null ? defaultAiDailyQuota : aiDailyQuota;
    }

    public void changeTo(
            Integer newAiDailyQuota,
            Boolean newIsBannedFromRatedContests,
            int defaultAiDailyQuota) {
        if (newAiDailyQuota != null
                && !Objects.equals(newAiDailyQuota, effectiveAiDailyQuota(defaultAiDailyQuota))) {
            aiDailyQuota = newAiDailyQuota;
        }
        if (newIsBannedFromRatedContests != null) {
            isBannedFromRatedContests = newIsBannedFromRatedContests;
        }
    }

    public String describe(int defaultAiDailyQuota) {
        String quota =
                aiDailyQuota == null
                        ? "default(" + defaultAiDailyQuota + ")"
                        : String.valueOf(aiDailyQuota);
        return "aiDailyQuota=" + quota + ";isBannedFromRatedContests=" + isBannedFromRatedContests;
    }
}
