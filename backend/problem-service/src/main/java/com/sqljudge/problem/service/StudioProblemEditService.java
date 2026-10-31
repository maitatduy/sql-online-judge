package com.sqljudge.problem.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.problem.dto.request.UpdateProblemRequest;
import com.sqljudge.problem.dto.response.StudioProblemResponse;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.Tag;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioProblemEditService {

    private final ProblemRepository problemRepository;
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public StudioProblemResponse get(UUID problemId) {
        return StudioProblemResponse.from(findAccessible(problemId));
    }

    @Transactional
    public StudioProblemResponse update(UUID problemId, UpdateProblemRequest request) {
        Problem problem = findAccessible(problemId);
        if (!problem.isEditableBySetter()) {
            throw new BusinessRuleException(
                    ErrorCode.PROBLEM_LOCKED, "Bài đang chờ duyệt hoặc đã xuất bản, chưa sửa được");
        }
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

    private Problem findAccessible(UUID problemId) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return problemRepository
                .findByIdAndDeletedAtIsNull(problemId)
                .filter(
                        problem ->
                                problem.getAuthorId().equals(user.id()) || user.hasRole(Role.ADMIN))
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.PROBLEM_NOT_FOUND, "Không tìm thấy bài tập"));
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
