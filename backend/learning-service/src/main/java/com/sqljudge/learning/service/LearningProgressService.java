package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.learning.dto.response.CourseProgressResponse;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.entity.Enrollment;
import com.sqljudge.learning.entity.LessonProgress;
import com.sqljudge.learning.repository.CourseRevisionRepository;
import com.sqljudge.learning.repository.EnrollmentRepository;
import com.sqljudge.learning.repository.LessonOutlineRow;
import com.sqljudge.learning.repository.LessonProgressRepository;
import com.sqljudge.learning.repository.LessonRepository;
import com.sqljudge.learning.repository.PublishedLessonRow;
import com.sqljudge.learning.repository.QuizOptionRepository;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningProgressService {

    private final CourseRevisionRepository courseRevisionRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CourseProgressResponse progress(String courseSlug) {
        return progressOf(publishedRevision(courseSlug), currentUserId());
    }

    @Transactional
    public CourseProgressResponse enroll(String courseSlug) {
        CourseRevision revision = publishedRevision(courseSlug);
        UUID userId = currentUserId();
        ensureEnrolled(revision.getCourse().getId(), userId);
        return progressOf(revision, userId);
    }

    @Transactional
    public CourseProgressResponse complete(String courseSlug, String lessonSlug) {
        if (quizOptionRepository.existsByQuestionLessonId(
                publishedLesson(courseSlug, lessonSlug).lessonId())) {
            throw new BusinessRuleException(
                    ErrorCode.LESSON_HAS_QUIZ, "Trả lời đúng hết câu hỏi để hoàn thành bài");
        }
        return recordCompletion(courseSlug, lessonSlug);
    }

    @Transactional
    public CourseProgressResponse recordCompletion(String courseSlug, String lessonSlug) {
        CourseRevision revision = publishedRevision(courseSlug);
        PublishedLessonRow lesson = publishedLesson(courseSlug, lessonSlug);
        UUID courseId = revision.getCourse().getId();
        UUID userId = currentUserId();
        ensureEnrolled(courseId, userId);
        if (!lessonProgressRepository.existsByUserIdAndLessonKey(userId, lesson.lessonKey())) {
            lessonProgressRepository.save(
                    LessonProgress.completed(
                            courseId, userId, lesson.lessonKey(), clock.instant()));
        }
        return progressOf(revision, userId);
    }

    private PublishedLessonRow publishedLesson(String courseSlug, String lessonSlug) {
        return lessonRepository
                .findPublished(courseSlug, lessonSlug)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.LESSON_NOT_FOUND, "Không tìm thấy bài giảng"));
    }

    private CourseRevision publishedRevision(String courseSlug) {
        return courseRevisionRepository
                .findPublishedBySlug(courseSlug)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.COURSE_NOT_FOUND, "Không tìm thấy khoá học"));
    }

    private void ensureEnrolled(UUID courseId, UUID userId) {
        if (!enrollmentRepository.existsByCourseIdAndUserId(courseId, userId)) {
            enrollmentRepository.save(Enrollment.of(courseId, userId));
        }
    }

    CourseProgressResponse progressOf(CourseRevision revision, UUID userId) {
        UUID courseId = revision.getCourse().getId();
        List<LessonOutlineRow> lessons =
                lessonRepository.findOutline(revision.getId()).stream()
                        .filter(row -> row.lessonKey() != null)
                        .toList();
        Set<UUID> completed = lessonProgressRepository.findCompletedLessonKeys(userId, courseId);
        List<UUID> completedInCourse =
                lessons.stream()
                        .map(LessonOutlineRow::lessonKey)
                        .filter(completed::contains)
                        .toList();
        String nextLessonSlug =
                lessons.stream()
                        .filter(row -> !completed.contains(row.lessonKey()))
                        .map(LessonOutlineRow::slug)
                        .findFirst()
                        .orElse(null);
        return new CourseProgressResponse(
                enrollmentRepository.existsByCourseIdAndUserId(courseId, userId),
                completedInCourse,
                lessons.size(),
                nextLessonSlug);
    }

    private static UUID currentUserId() {
        return SecurityUtils.requireCurrentUser().id();
    }
}
