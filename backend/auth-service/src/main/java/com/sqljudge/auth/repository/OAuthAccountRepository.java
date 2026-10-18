package com.sqljudge.auth.repository;

import com.sqljudge.auth.entity.OAuthAccount;
import com.sqljudge.auth.entity.SignupProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<OAuthAccount> findByProviderAndProviderUserId(
            SignupProvider provider, String providerUserId);
}
