package com.sqljudge.community.service;

import com.sqljudge.community.client.AuthServiceClient;
import com.sqljudge.community.client.UserName;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorNames {

    private final AuthServiceClient authServiceClient;

    public Map<UUID, String> of(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return authServiceClient.userNames(userIds).stream()
                .collect(
                        Collectors.toMap(
                                UserName::userId, UserName::username, (first, second) -> first));
    }
}
