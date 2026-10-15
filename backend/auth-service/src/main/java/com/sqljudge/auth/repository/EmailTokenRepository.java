package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.EmailToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {}
