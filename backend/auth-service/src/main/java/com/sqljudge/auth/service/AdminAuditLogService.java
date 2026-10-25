package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AuditActorsResponse;
import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.common.web.PageResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private static final int EXPORT_BATCH_SIZE = 500;

    private final AdminActionRepository adminActionRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogEntryResponse> search(AuditLogSearch search, int page, int size) {
        List<AdminActionType> actions = actionsOf(search);
        if (actions.isEmpty()) {
            return new PageResponse<>(List.of(), page, size, 0, 0);
        }
        return PageResponse.from(
                pageOf(search, actions, PageRequest.of(page - 1, size)), Function.identity());
    }

    @Transactional(readOnly = true)
    public AuditActorsResponse listActors() {
        return new AuditActorsResponse(adminActionRepository.findActorUsernames());
    }

    public void writeCsv(AuditLogSearch search, OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write(AuditLogCsv.header());
        List<AdminActionType> actions = actionsOf(search);
        Pageable pageable = PageRequest.of(0, EXPORT_BATCH_SIZE);
        while (!actions.isEmpty()) {
            Page<AuditLogEntryResponse> batch = pageOf(search, actions, pageable);
            for (AuditLogEntryResponse entry : batch) {
                writer.write(AuditLogCsv.row(entry));
            }
            writer.flush();
            if (!batch.hasNext()) {
                break;
            }
            pageable = batch.nextPageable();
        }
        writer.flush();
    }

    private Page<AuditLogEntryResponse> pageOf(
            AuditLogSearch search, List<AdminActionType> actions, Pageable pageable) {
        return adminActionRepository.searchAuditLog(
                search.targetUsername(),
                search.actorUsername(),
                actions,
                keywordPatternOf(search.keyword()),
                search.from(),
                pageable);
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
