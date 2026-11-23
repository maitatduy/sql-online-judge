package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.problem.dto.request.QuickEditProblemRequest;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProblemCommandService {

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;
    private final ProblemDetailCache problemDetailCache;

    @Transactional
    public void quickEdit(UUID problemId, QuickEditProblemRequest request) {
        Problem problem = published(problemId);
        problem.setTitle(request.title().trim());
        problem.setDifficulty(request.difficulty());
        problem.setTags(new HashSet<>(existingTags(request.tagSlugs())));
        evictAfterCommit(problem);
        log.info("admin_problem_edited problemId={}", problemId);
    }

    @Transactional
    public void addTags(UUID problemId, List<String> tagSlugs) {
        Problem problem = published(problemId);
        problem.getTags().addAll(existingTags(tagSlugs));
        evictAfterCommit(problem);
    }

    @Transactional
    public void changeDifficulty(UUID problemId, Difficulty difficulty) {
        Problem problem = published(problemId);
        problem.setDifficulty(difficulty);
        evictAfterCommit(problem);
    }

    private List<Tag> existingTags(List<String> tagSlugs) {
        List<String> slugs = tagSlugs.stream().distinct().toList();
        List<Tag> tags = tagRepository.findAllBySlugIn(slugs);
        if (tags.size() != slugs.size()) {
            throw new BusinessRuleException(ErrorCode.TAG_NOT_FOUND, "Có chủ đề không tồn tại");
        }
        return tags;
    }

    @Transactional
    public void hide(UUID problemId) {
        Problem problem = published(problemId);
        if (!problem.hide()) {
            throw visibilityUnchanged("Bài đang ẩn");
        }
        evictAfterCommit(problem);
        log.info("admin_problem_hidden problemId={}", problemId);
    }

    @Transactional
    public void restore(UUID problemId) {
        Problem problem = published(problemId);
        if (!problem.restoreVisibility()) {
            throw visibilityUnchanged("Bài không bị ẩn");
        }
        evictAfterCommit(problem);
        log.info("admin_problem_restored problemId={}", problemId);
    }

    @Transactional
    public void transferOwnership(UUID problemId, UUID newAuthorId) {
        Problem problem = published(problemId);
        problem.setAuthorId(newAuthorId);
        log.info("admin_problem_owner_changed problemId={}", problemId);
    }

    @Transactional(readOnly = true)
    public void requirePublished(UUID problemId) {
        published(problemId);
    }

    private Problem published(UUID problemId) {
        return problemRepository
                .findByIdAndDeletedAtIsNull(problemId)
                .filter(problem -> problem.getStatus() == ProblemStatus.PUBLISHED)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.PROBLEM_NOT_FOUND, "Không tìm thấy bài tập"));
    }

    private void evictAfterCommit(Problem problem) {
        String slug = problem.getSlug();
        UUID problemId = problem.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        problemDetailCache.evict(slug, problemId);
                    }
                });
    }

    private static BusinessRuleException visibilityUnchanged(String message) {
        return new BusinessRuleException(ErrorCode.PROBLEM_VISIBILITY_UNCHANGED, message);
    }
}
