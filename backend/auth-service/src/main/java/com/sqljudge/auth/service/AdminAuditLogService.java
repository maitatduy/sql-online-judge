package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AuditActorsResponse;
import com.sqljudge.auth.dto.response.AuditLogEntryResponse;
import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.common.web.PageResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private static final int EXPORT_BATCH_SIZE = 500;
    private static final String WHOLE_LOG = "Toàn bộ nhật ký";

    private final AdminActionRepository adminActionRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogEntryResponse> search(AuditLogSearch search, int page, int size) {
        return PageResponse.from(
                adminActionRepository.searchAuditLog(
                        search.targetUsername(),
                        search.actorUsername(),
                        actionsOf(search),
                        keywordPatternOf(search.keyword()),
                        search.from(),
                        PageRequest.of(page - 1, size)),
                Function.identity());
    }

    @Transactional(readOnly = true)
    public AuditActorsResponse listActors() {
        return new AuditActorsResponse(adminActionRepository.findActorUsernames());
    }

    @Transactional
    public void recordExport(AuditLogSearch search, UUID actorId) {
        adminActionRepository.save(AdminAction.exportAuditLog(actorId, describe(search)));
        log.info("admin_audit_log_exported actorId={}", actorId);
    }

    public void writeCsv(AuditLogSearch search, OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write(AuditLogCsv.header());
        List<AuditLogEntryResponse> batch = batchAfter(search, null);
        while (!batch.isEmpty()) {
            for (AuditLogEntryResponse entry : batch) {
                writer.write(AuditLogCsv.row(entry));
            }
            writer.flush();
            if (batch.size() < EXPORT_BATCH_SIZE) {
                break;
            }
            batch = batchAfter(search, batch.get(batch.size() - 1));
        }
        writer.flush();
    }

    private List<AuditLogEntryResponse> batchAfter(
            AuditLogSearch search, AuditLogEntryResponse last) {
        Instant beforeAt = last == null ? null : last.occurredAt();
        UUID beforeId = last == null ? null : last.id();
        return adminActionRepository.findAuditLogBatch(
                search.targetUsername(),
                search.actorUsername(),
                actionsOf(search),
                keywordPatternOf(search.keyword()),
                search.from(),
                beforeAt,
                beforeId,
                Limit.of(EXPORT_BATCH_SIZE));
    }

    private static List<AdminActionType> actionsOf(AuditLogSearch search) {
        return search.action() == null
                ? List.of(AdminActionType.values())
                : List.of(search.action());
    }

    private static String keywordPatternOf(String keyword) {
        return keyword == null || keyword.isBlank() ? null : LikePatterns.containing(keyword);
    }

    private static String describe(AuditLogSearch search) {
        List<String> filters = new ArrayList<>();
        addFilter(filters, "user", search.targetUsername());
        addFilter(filters, "actor", search.actorUsername());
        addFilter(filters, "action", search.action());
        addFilter(filters, "category", search.category());
        addFilter(filters, "q", search.keyword() == null ? null : search.keyword().strip());
        addFilter(filters, "from", search.from());
        return filters.isEmpty() ? WHOLE_LOG : String.join("; ", filters);
    }

    private static void addFilter(List<String> filters, String name, Object value) {
        if (value != null && !value.toString().isBlank()) {
            filters.add(name + "=" + value);
        }
    }
}
