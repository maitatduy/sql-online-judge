package com.sqljudge.community.repository;

import com.sqljudge.community.entity.Report;
import com.sqljudge.community.entity.ReportAction;
import com.sqljudge.community.entity.ReportStatus;
import com.sqljudge.community.entity.ReportTarget;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByTargetTypeAndTargetIdAndReporterId(
            ReportTarget targetType, UUID targetId, UUID reporterId);

    long countByTargetTypeAndTargetIdAndStatus(
            ReportTarget targetType, UUID targetId, ReportStatus status);

    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTarget targetType, UUID targetId);

    List<Report> findByTargetTypeAndTargetIdAndStatus(
            ReportTarget targetType, UUID targetId, ReportStatus status);

    List<Report> findByTargetIdInAndStatus(Collection<UUID> targetIds, ReportStatus status);

    @Query(
            value =
                    "SELECT new com.sqljudge.community.repository.ReportCaseRow(r.targetType,"
                            + " r.targetId, r.targetAuthorId, COUNT(r), MAX(r.createdAt))"
                            + " FROM Report r WHERE r.status = :status"
                            + " AND (:targetType IS NULL OR r.targetType = :targetType)"
                            + " GROUP BY r.targetType, r.targetId, r.targetAuthorId"
                            + " ORDER BY COUNT(r) DESC, MAX(r.createdAt) DESC",
            countQuery =
                    "SELECT COUNT(DISTINCT r.targetId) FROM Report r WHERE r.status = :status"
                            + " AND (:targetType IS NULL OR r.targetType = :targetType)")
    Page<ReportCaseRow> findCases(
            @Param("status") ReportStatus status,
            @Param("targetType") ReportTarget targetType,
            Pageable pageable);

    @Query(
            "SELECT COUNT(DISTINCT r.targetId) FROM Report r WHERE r.targetAuthorId = :authorId"
                    + " AND r.resolution = :removed")
    long countRemovedContentOf(
            @Param("authorId") UUID authorId, @Param("removed") ReportAction removed);
}
