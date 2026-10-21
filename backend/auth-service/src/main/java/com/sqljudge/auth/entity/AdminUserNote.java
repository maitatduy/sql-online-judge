package com.sqljudge.auth.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "admin_user_notes",
        indexes =
                @Index(
                        name = "idx_admin_user_notes_user_id_created_at",
                        columnList = "user_id, created_at"))
@Getter
@NoArgsConstructor
public class AdminUserNote extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Column(
            name = "author_id",
            nullable = false,
            updatable = false,
            columnDefinition = "BINARY(16)")
    private UUID authorId;

    @Column(name = "body", nullable = false, updatable = false, length = 1000)
    private String body;

    public static AdminUserNote of(User target, UUID authorId, String body) {
        AdminUserNote note = new AdminUserNote();
        note.userId = target.getId();
        note.authorId = authorId;
        note.body = body;
        return note;
    }
}
