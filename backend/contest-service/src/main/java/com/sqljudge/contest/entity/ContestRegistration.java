package com.sqljudge.contest.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "contest_registrations",
        indexes =
                @Index(
                        name = "uk_contest_registrations_contest_id_user_id",
                        columnList = "contest_id, user_id",
                        unique = true))
@Getter
@NoArgsConstructor
public class ContestRegistration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contest_id", nullable = false)
    private Contest contest;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", length = 100)
    private String displayName;

    public static ContestRegistration of(
            Contest contest, UUID userId, String username, String displayName) {
        ContestRegistration registration = new ContestRegistration();
        registration.contest = contest;
        registration.userId = userId;
        registration.username = username;
        registration.displayName = displayName;
        return registration;
    }
}
