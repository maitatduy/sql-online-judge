package com.sqljudge.auth.repository;

import com.sqljudge.auth.dto.response.UserNoteResponse;
import com.sqljudge.auth.entity.AdminUserNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserNoteRepository extends JpaRepository<AdminUserNote, UUID> {

    @Query(
            """
            SELECT new com.sqljudge.auth.dto.response.UserNoteResponse(
                n.id, author.username, n.body, n.createdAt)
            FROM AdminUserNote n
            JOIN User author ON author.id = n.authorId
            WHERE n.userId = :userId
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<UserNoteResponse> findLatestByUserId(@Param("userId") UUID userId, Limit limit);
}
