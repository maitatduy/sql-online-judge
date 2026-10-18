package com.sqljudge.auth.service;

import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.validation.UsernameRules;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsernameGenerator {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 30;
    private static final String FALLBACK_BASE = "user";
    private static final String DISALLOWED_CHARACTERS = "[^a-z0-9._]";
    private static final String PADDING = "_";

    private final UserRepository userRepository;

    public String generate(String source) {
        String base = normalize(source);
        String candidate = base;
        for (int suffix = 1; isTaken(candidate); suffix++) {
            String suffixText = String.valueOf(suffix);
            candidate =
                    base.substring(0, Math.min(base.length(), MAX_LENGTH - suffixText.length()))
                            + suffixText;
        }
        return candidate;
    }

    private boolean isTaken(String username) {
        return UsernameRules.isReserved(username) || userRepository.existsByUsername(username);
    }

    private static String normalize(String source) {
        if (source == null || source.isBlank()) {
            return FALLBACK_BASE;
        }
        String cleaned = source.toLowerCase(Locale.ROOT).replaceAll(DISALLOWED_CHARACTERS, PADDING);
        if (cleaned.length() > MAX_LENGTH) {
            return cleaned.substring(0, MAX_LENGTH);
        }
        StringBuilder padded = new StringBuilder(cleaned);
        while (padded.length() < MIN_LENGTH) {
            padded.append(PADDING);
        }
        return padded.toString();
    }
}
