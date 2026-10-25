package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

final class AdminUserSpecifications {

    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private AdminUserSpecifications() {}

    static Specification<User> createdNoLaterThan(Instant instant) {
        return (root, query, builder) ->
                builder.lessThanOrEqualTo(root.<Instant>get("createdAt"), instant);
    }

    static Specification<User> olderThan(User last) {
        return (root, query, builder) ->
                builder.or(
                        builder.lessThan(root.<Instant>get("createdAt"), last.getCreatedAt()),
                        builder.and(
                                builder.equal(root.get("createdAt"), last.getCreatedAt()),
                                builder.lessThan(root.<UUID>get("id"), last.getId())));
    }

    static Specification<User> matching(AdminUserSearch search, Instant now) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(
                    search.deleted()
                            ? builder.isNotNull(root.get("deletedAt"))
                            : builder.isNull(root.get("deletedAt")));
            if (search.q() != null && !search.q().isBlank()) {
                predicates.add(containsKeyword(root, builder, search.q()));
            }
            if (search.role() != null) {
                predicates.add(builder.equal(root.get("role"), search.role()));
            }
            if (search.status() != null) {
                Predicate banned = isBanned(root, builder, now);
                predicates.add(
                        search.status() == AccountStatus.BANNED ? banned : builder.not(banned));
            }
            if (search.emailVerified() != null) {
                predicates.add(
                        search.emailVerified()
                                ? builder.isNotNull(root.get("emailVerifiedAt"))
                                : builder.isNull(root.get("emailVerifiedAt")));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate containsKeyword(
            Root<User> root, CriteriaBuilder builder, String keyword) {
        String pattern = LikePatterns.containing(keyword);
        return builder.or(
                builder.like(root.get("username"), pattern, LikePatterns.ESCAPE),
                builder.like(root.get("email"), pattern, LikePatterns.ESCAPE));
    }

    private static Predicate isBanned(Root<User> root, CriteriaBuilder builder, Instant now) {
        return builder.and(
                builder.isNotNull(root.get("bannedAt")),
                builder.or(
                        builder.isNull(root.get("bannedUntil")),
                        builder.greaterThan(root.<Instant>get("bannedUntil"), now)));
    }
}
