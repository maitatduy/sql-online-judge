package com.sqljudge.problem.repository;

import com.sqljudge.problem.entity.Tag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findAllByOrderByNameAsc();

    List<Tag> findAllBySlugIn(Collection<String> slugs);

    @Query(
            "SELECT t.slug AS slug, COUNT(p) AS total FROM Problem p JOIN p.tags t"
                    + " WHERE p.deletedAt IS NULL GROUP BY t.slug")
    List<TagUsageRow> countLiveProblemsPerTag();
}
