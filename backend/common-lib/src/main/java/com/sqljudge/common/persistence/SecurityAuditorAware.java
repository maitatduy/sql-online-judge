package com.sqljudge.common.persistence;

import com.sqljudge.common.security.SecurityUtils;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;

public class SecurityAuditorAware implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return SecurityUtils.currentUserId();
    }
}
