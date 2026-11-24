package com.sqljudge.notification.service;

import com.sqljudge.notification.dto.response.PreferenceResponse;
import com.sqljudge.notification.entity.NotificationPreference;
import com.sqljudge.notification.repository.NotificationPreferenceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferences {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public PreferenceResponse get(UUID userId) {
        return new PreferenceResponse(
                preferenceRepository
                        .findByUserId(userId)
                        .map(NotificationPreference::isEmailEnabled)
                        .orElse(true));
    }

    @Transactional
    public PreferenceResponse update(UUID userId, boolean emailEnabled) {
        preferenceRepository
                .findByUserId(userId)
                .ifPresentOrElse(
                        preference -> preference.changeEmail(emailEnabled),
                        () ->
                                preferenceRepository.save(
                                        NotificationPreference.of(userId, emailEnabled)));
        return new PreferenceResponse(emailEnabled);
    }
}
