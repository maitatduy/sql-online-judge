package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.learning.dto.response.AdminCourseItem;
import com.sqljudge.learning.dto.response.StudioCourseResponse;
import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.CourseStatus;
import com.sqljudge.learning.repository.CourseRepository;
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
public class AdminCourseCatalogService {

    private final CourseRepository courseRepository;
    private final StudioCourseAccess courseAccess;

    @Transactional(readOnly = true)
    public PageResponse<AdminCourseItem> catalog(CourseStatus status, int page, int size) {
        return PageResponse.from(
                courseRepository.findCatalog(status, PageRequest.of(page - 1, size)),
                Function.identity());
    }

    @Transactional
    public StudioCourseResponse archive(UUID courseId) {
        Course course = liveCourse(courseId);
        if (!course.isPublished()) {
            throw new BusinessRuleException(
                    ErrorCode.COURSE_NOT_PUBLISHED, "Chỉ ẩn được khoá đang xuất bản");
        }
        course.archive();
        log.info("course_archived courseId={}", course.getId());
        return StudioCourseResponse.of(course, courseAccess.workingRevisionOf(course));
    }

    @Transactional
    public StudioCourseResponse restore(UUID courseId) {
        Course course = liveCourse(courseId);
        if (!course.isArchived()) {
            throw new BusinessRuleException(
                    ErrorCode.COURSE_NOT_ARCHIVED, "Khoá không ở trạng thái đã ẩn");
        }
        course.restore();
        log.info("course_restored courseId={}", course.getId());
        return StudioCourseResponse.of(course, courseAccess.workingRevisionOf(course));
    }

    private Course liveCourse(UUID courseId) {
        return courseRepository
                .findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.COURSE_NOT_FOUND, "Không tìm thấy khoá học"));
    }
}
