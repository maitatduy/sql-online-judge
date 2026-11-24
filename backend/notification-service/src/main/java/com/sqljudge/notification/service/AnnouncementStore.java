package com.sqljudge.notification.service;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.notification.dto.response.AnnouncementResponse;
import com.sqljudge.notification.entity.Announcement;
import com.sqljudge.notification.repository.AnnouncementRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementStore {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public AnnouncementResponse create(String title, String link) {
        return AnnouncementResponse.from(
                announcementRepository.save(Announcement.draft(title.trim(), link)));
    }

    @Transactional
    public void markDelivered(UUID announcementId, int recipients) {
        announcementRepository
                .findById(announcementId)
                .ifPresent(announcement -> announcement.delivered(recipients));
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> list(int page, int size) {
        return PageResponse.from(
                announcementRepository.findAllByOrderByCreatedAtDesc(
                        PageRequest.of(page - 1, size)),
                AnnouncementResponse::from);
    }
}
