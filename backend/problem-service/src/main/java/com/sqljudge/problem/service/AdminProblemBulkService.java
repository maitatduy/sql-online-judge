package com.sqljudge.problem.service;

import com.sqljudge.common.exception.AppException;
import com.sqljudge.problem.dto.request.BulkUpdateProblemsRequest;
import com.sqljudge.problem.dto.response.BulkUpdateProblemsResponse;
import com.sqljudge.problem.dto.response.BulkUpdateProblemsResponse.Failure;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProblemBulkService {

    private final AdminProblemCommandService commandService;
    private final AdminProblemActions problemActions;

    public BulkUpdateProblemsResponse update(BulkUpdateProblemsRequest request) {
        List<UUID> succeeded = new ArrayList<>();
        List<Failure> failed = new ArrayList<>();
        for (UUID problemId : request.problemIds().stream().distinct().toList()) {
            try {
                apply(problemId, request);
                succeeded.add(problemId);
            } catch (AppException failure) {
                failed.add(new Failure(problemId, failure.getCode().name(), failure.getMessage()));
            }
        }
        log.info(
                "admin_problems_bulk_updated succeeded={} failed={}",
                succeeded.size(),
                failed.size());
        return new BulkUpdateProblemsResponse(succeeded, failed);
    }

    private void apply(UUID problemId, BulkUpdateProblemsRequest request) {
        if (request.hasTags()) {
            commandService.addTags(problemId, request.addTagSlugs());
        }
        if (request.difficulty() != null) {
            commandService.changeDifficulty(problemId, request.difficulty());
        }
        if (request.hide()) {
            problemActions.hide(problemId);
        }
    }
}
