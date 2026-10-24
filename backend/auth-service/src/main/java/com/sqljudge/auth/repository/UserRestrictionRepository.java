package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.UserRestriction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRestrictionRepository extends JpaRepository<UserRestriction, UUID> {

    Optional<UserRestriction> findByUserId(UUID userId);
}
