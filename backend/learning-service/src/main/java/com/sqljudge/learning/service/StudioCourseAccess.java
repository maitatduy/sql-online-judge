package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.learning.entity.Chapter;
import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.entity.Lesson;
import com.sqljudge.learning.repository.ChapterRepository;
import com.sqljudge.learning.repository.CourseRepository;
import com.sqljudge.learning.repository.CourseRevisionRepository;
import com.sqljudge.learning.repository.LessonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class StudioCourseAccess {

    private final CourseRepository courseRepository;
    private final CourseRevisionRepository courseRevisionRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;

    Course accessible(UUID courseId) {
        return courseRepository
                .findByIdAndDeletedAtIsNull(courseId)
                .filter(StudioCourseAccess::isMine)
                .orElseThrow(StudioCourseAccess::courseNotFound);
    }

    CourseRevision workingRevisionOf(Course course) {
        UUID revisionId =
                course.getDraftRevisionId() != null
                        ? course.getDraftRevisionId()
                        : course.getPublishedRevisionId();
        return courseRevisionRepository
                .findById(revisionId)
                .orElseThrow(StudioCourseAccess::courseNotFound);
    }

    Chapter editableChapter(UUID chapterId) {
        Chapter chapter =
                chapterRepository
                        .findWithCourse(chapterId)
                        .filter(found -> isMine(found.getRevision().getCourse()))
                        .orElseThrow(StudioCourseAccess::courseNotFound);
        editableRevision(chapter.getRevision());
        return chapter;
    }

    Lesson accessibleLesson(UUID lessonId) {
        return lessonRepository
                .findWithCourse(lessonId)
                .filter(found -> isMine(found.getChapter().getRevision().getCourse()))
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.LESSON_NOT_FOUND, "Không tìm thấy bài giảng"));
    }

    Lesson editableLesson(UUID lessonId) {
        Lesson lesson = accessibleLesson(lessonId);
        editableRevision(lesson.getChapter().getRevision());
        return lesson;
    }

    CourseRevision editableRevision(CourseRevision revision) {
        if (!revision.isEditable()) {
            throw new BusinessRuleException(
                    ErrorCode.COURSE_LOCKED, "Khoá đang chờ duyệt nên chưa sửa được");
        }
        return revision;
    }

    static boolean isMine(Course course) {
        CurrentUser user = SecurityUtils.requireCurrentUser();
        return course.getDeletedAt() == null
                && (course.getAuthorId().equals(user.id()) || user.hasRole(Role.ADMIN));
    }

    static NotFoundException courseNotFound() {
        return new NotFoundException(ErrorCode.COURSE_NOT_FOUND, "Không tìm thấy khoá học");
    }
}
