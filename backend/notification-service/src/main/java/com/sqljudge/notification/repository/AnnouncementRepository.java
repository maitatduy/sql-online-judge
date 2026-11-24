package com.sqljudge.notification.repository;

import com.sqljudge.notification.entity.Announcement;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
