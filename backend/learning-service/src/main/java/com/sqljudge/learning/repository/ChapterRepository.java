package com.sqljudge.learning.repository;

import com.sqljudge.learning.entity.Chapter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findByRevisionIdOrderByPosition(UUID revisionId);

    @Query("SELECT COALESCE(MAX(c.position), 0) FROM Chapter c WHERE c.revision.id = :revisionId")
    int findLastPosition(@Param("revisionId") UUID revisionId);

    @Query(
            "SELECT c FROM Chapter c JOIN FETCH c.revision r JOIN FETCH r.course"
                    + " WHERE c.id = :chapterId")
    Optional<Chapter> findWithCourse(@Param("chapterId") UUID chapterId);
}
