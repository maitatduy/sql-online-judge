package com.sqljudge.submission.service;

import com.sqljudge.submission.client.AuthServiceClient;
import com.sqljudge.submission.client.UserName;
import com.sqljudge.submission.dto.response.AdminSubmissionResponse;
import com.sqljudge.submission.dto.response.JudgeAttentionResponse;
import com.sqljudge.submission.dto.response.JudgeQueueResponse;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.entity.Verdict;
import com.sqljudge.submission.repository.AdminSubmissionRepository;
import com.sqljudge.submission.repository.AdminSubmissionRow;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JudgeQueueService {

    static final Duration STUCK_AFTER = Duration.ofMinutes(2);
    private static final Duration HOUR = Duration.ofHours(1);
    private static final Duration DAY = Duration.ofDays(1);
    private static final Duration ERROR_WINDOW = Duration.ofDays(7);
    private static final int MAX_ATTENTION_ITEMS = 50;

    private final AdminSubmissionRepository adminSubmissionRepository;
    private final AuthServiceClient authServiceClient;
    private final RejudgeWriter rejudgeWriter;
    private final Clock clock;

    @Transactional(readOnly = true)
    public JudgeQueueResponse queue() {
        Instant now = clock.instant();
        return new JudgeQueueResponse(
                adminSubmissionRepository.countByStatusAndDeletedAtIsNull(SubmissionStatus.PENDING),
                adminSubmissionRepository.countByStatusAndDeletedAtIsNull(SubmissionStatus.JUDGING),
                adminSubmissionRepository.countStuck(now.minus(STUCK_AFTER)),
                adminSubmissionRepository
                        .countByVerdictAndJudgedAtGreaterThanEqualAndDeletedAtIsNull(
                                Verdict.SE, now.minus(DAY)),
                adminSubmissionRepository
                        .countByStatusAndJudgedAtGreaterThanEqualAndDeletedAtIsNull(
                                SubmissionStatus.COMPLETED, now.minus(HOUR)),
                (int) STUCK_AFTER.toMinutes());
    }

    public JudgeAttentionResponse attention() {
        Instant now = clock.instant();
        List<AdminSubmissionRow> rows =
                adminSubmissionRepository.findNeedingAttention(
                        now.minus(STUCK_AFTER),
                        now.minus(ERROR_WINDOW),
                        Limit.of(MAX_ATTENTION_ITEMS));
        Map<UUID, UserName> names = namesOf(rows);
        return new JudgeAttentionResponse(
                rows.stream()
                        .map(row -> AdminSubmissionResponse.of(row, names.get(row.userId())))
                        .toList());
    }

    public void rejudge(UUID submissionId) {
        rejudgeWriter.rejudgeOne(submissionId, clock.instant().minus(STUCK_AFTER));
    }

    private Map<UUID, UserName> namesOf(List<AdminSubmissionRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        return authServiceClient
                .userNames(rows.stream().map(AdminSubmissionRow::userId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(UserName::userId, Function.identity(), (a, b) -> a));
    }
}
