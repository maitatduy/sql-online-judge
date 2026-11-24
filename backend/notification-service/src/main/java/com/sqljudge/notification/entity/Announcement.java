package com.sqljudge.notification.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "announcements",
        indexes = @Index(name = "idx_announcements_created_at", columnList = "created_at"))
@Getter
@NoArgsConstructor
public class Announcement extends BaseEntity {

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "link", length = 300)
    private String link;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Column(name = "is_delivered", nullable = false)
    private boolean delivered;

    public static Announcement draft(String title, String link) {
        Announcement announcement = new Announcement();
        announcement.title = title;
        announcement.link = link;
        return announcement;
    }

    public void delivered(int recipients) {
        recipientCount = recipients;
        delivered = true;
    }
}
