package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.learning.dto.response.CourseReviewQueueItem;
import com.sqljudge.learning.dto.response.StudioCourseResponse;
import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.CourseReview;
import com.sqljudge.learning.entity.CourseReviewDecision;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.repository.CourseRepository;
import com.sqljudge.learning.repository.CourseReviewRepository;
import com.sqljudge.learning.repository.CourseRevisionRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCourseReviewService {

    private final CourseRepository courseRepository;
    private final CourseRevisionRepository courseRevisionRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<CourseReviewQueueItem> queue(int page, int size) {
        return PageResponse.from(
                courseRevisionRepository.findReviewQueue(PageRequest.of(page - 1, size)),
                Function.identity());
    }

    @Transactional
    public StudioCourseResponse approve(UUID courseId) {
        Course course = liveCourse(courseId);
        CourseRevision draft = pendingRevisionOf(course);
        if (course.getPublishedRevisionId() != null) {
            courseRevisionRepository
                    .findById(course.getPublishedRevisionId())
                    .ifPresent(CourseRevision::supersede);
        }
        draft.publish(clock.instant());
        course.publish(draft);
        CourseReview review = record(draft, CourseReviewDecision.APPROVED, null);
        log.info("course_published courseId={} revisionId={}", course.getId(), draft.getId());
        return StudioCourseResponse.of(course, draft).withReview(review);
    }

    @Transactional
    public StudioCourseResponse requestChanges(UUID courseId, String reason) {
        Course course = liveCourse(courseId);
        CourseRevision draft = pendingRevisionOf(course);
        draft.returnToDraft();
        CourseReview review = record(draft, CourseReviewDecision.CHANGES_REQUESTED, reason.trim());
        log.info("course_changes_requested courseId={}", course.getId());
        return StudioCourseResponse.of(course, draft).withReview(review);
    }

    private CourseReview record(
            CourseRevision revision, CourseReviewDecision decision, String reason) {
        return courseReviewRepository.saveAndFlush(
                CourseReview.of(revision, SecurityUtils.requireCurrentUser().id())
                        .decide(decision, reason));
    }

    private Course liveCourse(UUID courseId) {
        return courseRepository
                .findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.COURSE_NOT_FOUND, "Không tìm thấy khoá học"));
    }

    private CourseRevision pendingRevisionOf(Course course) {
        return Optional.ofNullable(course.getDraftRevisionId())
                .flatMap(courseRevisionRepository::findById)
                .filter(CourseRevision::isInReview)
                .orElseThrow(
                        () ->
                                new BusinessRuleException(
                                        ErrorCode.COURSE_NOT_IN_REVIEW,
                                        "Khoá không ở trạng thái chờ duyệt"));
    }
}
