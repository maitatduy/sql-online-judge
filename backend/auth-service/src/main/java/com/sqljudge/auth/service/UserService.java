package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.UsernameAvailabilityResponse;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.validation.UsernameRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UsernameAvailabilityResponse checkUsernameAvailability(String username) {
        boolean isAvailable =
                !UsernameRules.isReserved(username) && !userRepository.existsByUsername(username);
        return new UsernameAvailabilityResponse(isAvailable);
    }
}
