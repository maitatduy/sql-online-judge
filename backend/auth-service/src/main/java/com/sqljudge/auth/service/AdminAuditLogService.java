package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.common.web.PageResponse;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminActionRepository adminActionRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogEntryResponse> search(AuditLogSearch search, int page, int size) {
        return PageResponse.from(
                adminActionRepository.searchAuditLog(
                        search.targetUsername(),
                        search.actorUsername(),
                        search.action(),
                        search.from(),
                        PageRequest.of(page - 1, size)),
                Function.identity());
    }
}
