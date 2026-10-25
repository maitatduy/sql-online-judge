package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AuditActorsResponse;
import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.common.web.PageResponse;
import java.util.List;
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
        List<AdminActionType> actions = actionsOf(search);
        if (actions.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        return PageResponse.from(
                adminActionRepository.searchAuditLog(
                        search.targetUsername(),
                        search.actorUsername(),
                        actions,
                        keywordPatternOf(search.keyword()),
                        search.from(),
                        PageRequest.of(page - 1, size)),
                Function.identity());
    }

    @Transactional(readOnly = true)
    public AuditActorsResponse listActors() {
        return new AuditActorsResponse(adminActionRepository.findActorUsernames());
    }

    private static List<AdminActionType> actionsOf(AuditLogSearch search) {
        List<AdminActionType> inCategory =
                search.category() == null
                        ? List.of(AdminActionType.values())
                        : AdminActionType.inCategory(search.category());
        if (search.action() == null) {
            return inCategory;
        }
        return inCategory.contains(search.action()) ? List.of(search.action()) : List.of();
    }

    private static String keywordPatternOf(String keyword) {
        return keyword == null || keyword.isBlank() ? null : LikePatterns.containing(keyword);
    }
}
