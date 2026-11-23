package com.sqljudge.contest.repository;

import com.sqljudge.contest.entity.UserRating;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRatingRepository extends JpaRepository<UserRating, UUID> {

    List<UserRating> findByUserIdIn(Collection<UUID> userIds);
}
