package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.learning.dto.request.CreateCourseRequest;
import com.sqljudge.learning.dto.request.UpdateCourseRequest;
import com.sqljudge.learning.dto.response.StudioCourseResponse;
import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.repository.CourseRepository;
import com.sqljudge.learning.repository.CourseRevisionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioCourseService {

    private final CourseRepository courseRepository;
    private final CourseRevisionRepository courseRevisionRepository;

    @Transactional
    public StudioCourseResponse create(CreateCourseRequest request) {
        if (courseRepository.existsBySlug(request.slug())) {
            throw new ConflictException(ErrorCode.SLUG_TAKEN, "Slug đã được khoá khác dùng");
        }
        Course course =
                courseRepository.saveAndFlush(
                        Course.draft(request.slug(), SecurityUtils.requireCurrentUser().id()));
        CourseRevision revision = CourseRevision.draftOf(course);
        revision.describe(
                request.title(),
                request.summary(),
                request.difficulty(),
                request.estimatedMinutes());
        revision = courseRevisionRepository.saveAndFlush(revision);
        course.setDraftRevisionId(revision.getId());
        return StudioCourseResponse.of(course, revision);
    }

    @Transactional(readOnly = true)
    public List<StudioCourseResponse> listMine() {
        UUID authorId = SecurityUtils.requireCurrentUser().id();
        return courseRepository
                .findTop100ByAuthorIdAndDeletedAtIsNullOrderByUpdatedAtDesc(authorId)
                .stream()
                .map(course -> StudioCourseResponse.of(course, workingRevisionOf(course)))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudioCourseResponse get(UUID courseId) {
        Course course = accessible(courseId);
        return StudioCourseResponse.of(course, workingRevisionOf(course));
    }

    @Transactional
    public StudioCourseResponse update(UUID courseId, UpdateCourseRequest request) {
        Course course = accessible(courseId);
        CourseRevision revision = workingRevisionOf(course);
        if (!revision.isEditable()) {
            throw new BusinessRuleException(
                    ErrorCode.COURSE_LOCKED, "Khoá đang chờ duyệt nên chưa sửa được");
        }
        revision.describe(
                request.title(),
                request.summary(),
                request.difficulty(),
                request.estimatedMinutes());
        return StudioCourseResponse.of(course, courseRevisionRepository.saveAndFlush(revision));
    }

    private Course accessible(UUID courseId) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return courseRepository
                .findByIdAndDeletedAtIsNull(courseId)
                .filter(
                        course ->
                                course.getAuthorId().equals(user.id()) || user.hasRole(Role.ADMIN))
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.COURSE_NOT_FOUND, "Không tìm thấy khoá học"));
    }

    private CourseRevision workingRevisionOf(Course course) {
        UUID revisionId =
                course.getDraftRevisionId() != null
                        ? course.getDraftRevisionId()
                        : course.getPublishedRevisionId();
        return courseRevisionRepository
                .findById(revisionId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.COURSE_NOT_FOUND, "Không tìm thấy khoá học"));
    }
}
