package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.AdminAction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActionRepository extends JpaRepository<AdminAction, UUID> {}
