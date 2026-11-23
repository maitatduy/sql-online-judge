package com.sqljudge.community.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "bookmarks",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_bookmarks_user_id_post_id",
                        columnNames = {"user_id", "post_id"}))
@Getter
@NoArgsConstructor
public class Bookmark extends BaseEntity {

    @Column(name = "post_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID postId;

    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    public static Bookmark of(UUID postId, UUID userId) {
        Bookmark bookmark = new Bookmark();
        bookmark.postId = postId;
        bookmark.userId = userId;
        return bookmark;
    }
}
