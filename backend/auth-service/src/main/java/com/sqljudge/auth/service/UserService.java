package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.UsernameAvailabilityResponse;
import com.sqljudge.auth.repository.UserRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> RESERVED_USERNAMES = Set.of("me", "availability");

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UsernameAvailabilityResponse checkUsernameAvailability(String username) {
        boolean isAvailable =
                !RESERVED_USERNAMES.contains(username)
                        && !userRepository.existsByUsername(username);
        return new UsernameAvailabilityResponse(isAvailable);
    }
}
