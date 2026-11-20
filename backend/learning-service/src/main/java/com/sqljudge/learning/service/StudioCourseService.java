package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.learning.dto.request.CreateCourseRequest;
import com.sqljudge.learning.dto.request.UpdateCourseRequest;
import com.sqljudge.learning.dto.response.StudioCourseResponse;
import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.repository.CourseRepository;
import com.sqljudge.learning.repository.CourseReviewRepository;
import com.sqljudge.learning.repository.CourseRevisionRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudioCourseService {

    private final CourseRepository courseRepository;
    private final CourseRevisionRepository courseRevisionRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final StudioCourseAccess courseAccess;
    private final Clock clock;

    @Transactional
    public StudioCourseResponse create(CreateCourseRequest request) {
        if (courseRepository.existsBySlug(request.slug())) {
            throw new ConflictException(ErrorCode.SLUG_TAKEN, "Slug đã được khoá khác dùng");
        }
        Course course =
                courseRepository.saveAndFlush(
                        Course.draft(request.slug(), SecurityUtils.requireCurrentUser().id()));
        CourseRevision revision = CourseRevision.draftOf(course);
        revision.describe(request.title(), request.summary(), request.difficulty());
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
                .map(
                        course ->
                                StudioCourseResponse.of(
                                        course, courseAccess.workingRevisionOf(course)))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudioCourseResponse get(UUID courseId) {
        Course course = courseAccess.accessible(courseId);
        return StudioCourseResponse.of(course, courseAccess.workingRevisionOf(course))
                .withReview(
                        courseReviewRepository
                                .findFirstByCourseIdOrderByCreatedAtDesc(courseId)
                                .orElse(null));
    }

    @Transactional
    public StudioCourseResponse update(UUID courseId, UpdateCourseRequest request) {
        Course course = courseAccess.accessible(courseId);
        CourseRevision revision =
                courseAccess.editableRevision(courseAccess.workingRevisionOf(course));
        revision.describe(request.title(), request.summary(), request.difficulty());
        return StudioCourseResponse.of(course, courseRevisionRepository.saveAndFlush(revision));
    }

    @Transactional
    public StudioCourseResponse submit(UUID courseId) {
        Course course = courseAccess.accessible(courseId);
        CourseRevision revision =
                courseAccess.editableRevision(courseAccess.workingRevisionOf(course));
        if (revision.getLessonCount() == 0) {
            throw new BusinessRuleException(
                    ErrorCode.COURSE_EMPTY, "Khoá cần ít nhất một bài giảng trước khi gửi duyệt");
        }
        revision.submit(clock.instant());
        log.info("course_submitted courseId={} revisionId={}", course.getId(), revision.getId());
        return StudioCourseResponse.of(course, courseRevisionRepository.saveAndFlush(revision));
    }
}
