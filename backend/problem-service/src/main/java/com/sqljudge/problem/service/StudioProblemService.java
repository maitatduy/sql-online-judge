package com.sqljudge.problem.service;

import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.problem.dto.request.CreateProblemRequest;
import com.sqljudge.problem.dto.response.StudioProblemSummaryResponse;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.StatusCountRow;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioProblemService {

    private static final int DEFAULT_TIME_LIMIT_MS = 2000;
    private static final int FIRST_REVISION = 1;
    private static final int FIRST_SLUG_SUFFIX = 2;

    private final ProblemRepository problemRepository;

    @Transactional
    public StudioProblemSummaryResponse createDraft(CreateProblemRequest request) {
        String title = request.title().trim();
        Problem problem = new Problem();
        problem.setTitle(title);
        problem.setSlug(uniqueSlug(SlugGenerator.slugOf(title)));
        problem.setDifficulty(Difficulty.MEDIUM);
        problem.setStatus(ProblemStatus.DRAFT);
        problem.setAuthorId(SecurityUtils.requireCurrentUser().id());
        problem.setStatementMarkdown("");
        problem.setStarterCode("");
        problem.setTimeLimitMs(DEFAULT_TIME_LIMIT_MS);
        ProblemRevision draft = problem.addRevision(FIRST_REVISION);
        problem.setDraftRevision(draft);
        return StudioProblemSummaryResponse.from(problemRepository.saveAndFlush(problem));
    }

    @Transactional(readOnly = true)
    public PageResponse<StudioProblemSummaryResponse> listOwned(
            ProblemStatus status, String keyword, int page, int size) {
        return PageResponse.from(
                problemRepository.findOwned(
                        SecurityUtils.requireCurrentUser().id(),
                        status,
                        LikePatterns.containingIgnoreCase(keyword),
                        PageRequest.of(page - 1, size)),
                StudioProblemSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Map<ProblemStatus, Long> countOwnedByStatus() {
        UUID authorId = SecurityUtils.requireCurrentUser().id();
        Map<ProblemStatus, Long> counts = new EnumMap<>(ProblemStatus.class);
        for (ProblemStatus status : ProblemStatus.values()) {
            counts.put(status, 0L);
        }
        for (StatusCountRow row : problemRepository.countOwnedByStatus(authorId)) {
            counts.put(row.getStatus(), row.getTotal());
        }
        return counts;
    }

    private String uniqueSlug(String base) {
        Set<String> taken = new HashSet<>(problemRepository.findSlugsLike(base, base + "-%"));
        if (!taken.contains(base)) {
            return base;
        }
        int suffix = FIRST_SLUG_SUFFIX;
        while (taken.contains(base + "-" + suffix)) {
            suffix += 1;
        }
        return base + "-" + suffix;
    }
}
