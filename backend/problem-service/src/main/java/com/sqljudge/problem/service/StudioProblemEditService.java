package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.problem.dto.request.ChooseProblemDatasetRequest;
import com.sqljudge.problem.dto.request.UpdateProblemRequest;
import com.sqljudge.problem.dto.response.StudioProblemResponse;
import com.sqljudge.problem.entity.Dataset;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.TagRepository;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class StudioProblemEditService {

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final ProblemDetailCache problemDetailCache;
    private final ProblemAccess problemAccess;
    private final DatasetAccess datasetAccess;
    private final DatasetFreezer datasetFreezer;
    private final Clock clock;

    @Transactional(readOnly = true)
    public StudioProblemResponse get(UUID problemId) {
        return StudioProblemResponse.from(problemAccess.accessible(problemId));
    }

    @Transactional
    public StudioProblemResponse update(UUID problemId, UpdateProblemRequest request) {
        Problem problem = problemAccess.editable(problemId);
        if (request.title() != null) {
            problem.setTitle(request.title().trim());
        }
        if (request.slug() != null) {
            changeSlug(problem, request.slug());
        }
        if (request.difficulty() != null) {
            problem.setDifficulty(request.difficulty());
        }
        if (request.tagSlugs() != null) {
            problem.setTags(tagsOf(request.tagSlugs()));
        }
        if (request.statementMarkdown() != null) {
            problem.setStatementMarkdown(request.statementMarkdown());
        }
        return StudioProblemResponse.from(problemRepository.saveAndFlush(problem));
    }

    @Transactional
    public StudioProblemResponse chooseDataset(
            UUID problemId, ChooseProblemDatasetRequest request) {
        Problem problem = problemAccess.editable(problemId);
        Dataset dataset = datasetAccess.editable(request.datasetId());
        problem.getDraftRevision().useDatasetVersion(datasetFreezer.freezeLatest(dataset));
        return StudioProblemResponse.from(problemRepository.saveAndFlush(problem));
    }

    @Transactional
    public void delete(UUID problemId) {
        Problem problem = problemAccess.accessible(problemId);
        boolean isAdmin = SecurityUtils.requireCurrentUser().hasRole(Role.ADMIN);
        if (!problem.isEditableBySetter() && !isAdmin) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_LOCKED, "Chỉ xoá được bài nháp hoặc bài cần sửa");
        }
        problem.setDeletedAt(clock.instant());
        problemRepository.saveAndFlush(problem);
        evictDetailAfterCommit(problem.getSlug());
    }

    private void evictDetailAfterCommit(String slug) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        problemDetailCache.evict(slug);
                    }
                });
    }

    private void changeSlug(Problem problem, String slug) {
        if (problemRepository.existsBySlugAndIdNot(slug, problem.getId())) {
            throw new ConflictException(ErrorCode.SLUG_TAKEN, "Slug đã được bài khác dùng");
        }
        problem.setSlug(slug);
    }

    private Set<Tag> tagsOf(List<String> slugs) {
        Set<String> requested = new HashSet<>(slugs);
        List<Tag> tags = tagRepository.findAllBySlugIn(requested);
        if (tags.size() != requested.size()) {
            throw new InvalidFieldException("tagSlugs", "Có chủ đề không tồn tại");
        }
        return new HashSet<>(tags);
    }
}
