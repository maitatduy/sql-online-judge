package com.sqljudge.notification.repository;

import com.sqljudge.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    List<Notification> findByUserIdOrderByIdDesc(UUID userId, Pageable pageable);

    List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(
            UUID userId, UUID before, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query(
            "UPDATE Notification n SET n.readAt = :now"
                    + " WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
