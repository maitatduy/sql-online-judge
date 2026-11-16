package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.learning.dto.request.LessonRequest;
import com.sqljudge.learning.dto.response.StudioLessonResponse;
import com.sqljudge.learning.entity.Chapter;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.entity.Lesson;
import com.sqljudge.learning.repository.LessonCellRepository;
import com.sqljudge.learning.repository.LessonRepository;
import com.sqljudge.learning.repository.LessonTotals;
import com.sqljudge.learning.repository.QuizOptionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioLessonService {

    private final LessonRepository lessonRepository;
    private final LessonCellRepository lessonCellRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final StudioCourseAccess courseAccess;

    @Transactional
    public StudioLessonResponse add(UUID chapterId, LessonRequest request) {
        Chapter chapter = courseAccess.editableChapter(chapterId);
        requireFreeSlug(chapter, request.slug(), null);
        Lesson lesson = Lesson.of(chapter, lessonRepository.findLastPosition(chapterId) + 1);
        Lesson saved = lessonRepository.save(writeInto(lesson, request));
        recount(chapter.getRevision());
        return StudioLessonResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public StudioLessonResponse get(UUID lessonId) {
        return StudioLessonResponse.of(accessibleLesson(lessonId));
    }

    @Transactional
    public StudioLessonResponse update(UUID lessonId, LessonRequest request) {
        Lesson lesson = editableLesson(lessonId);
        requireFreeSlug(lesson.getChapter(), request.slug(), lessonId);
        Lesson saved = lessonRepository.save(writeInto(lesson, request));
        recount(lesson.getChapter().getRevision());
        return StudioLessonResponse.of(saved);
    }

    @Transactional
    public void delete(UUID lessonId) {
        Lesson lesson = editableLesson(lessonId);
        quizOptionRepository.deleteByLessonId(lessonId);
        quizOptionRepository.deleteQuestionsByLessonId(lessonId);
        lessonCellRepository.deleteByLessonId(lessonId);
        lessonRepository.delete(lesson);
        recount(lesson.getChapter().getRevision());
    }

    private static Lesson writeInto(Lesson lesson, LessonRequest request) {
        lesson.write(request.slug(), request.title(), request.body());
        lesson.setEstimatedMinutes(request.estimatedMinutes());
        return lesson;
    }

    private void recount(CourseRevision revision) {
        LessonTotals totals = lessonRepository.findTotals(revision.getId());
        revision.recount(Math.toIntExact(totals.lessonCount()), Math.toIntExact(totals.minutes()));
    }

    private Lesson accessibleLesson(UUID lessonId) {
        return lessonRepository
                .findWithCourse(lessonId)
                .filter(
                        found ->
                                StudioCourseAccess.isMine(
                                        found.getChapter().getRevision().getCourse()))
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.LESSON_NOT_FOUND, "Không tìm thấy bài giảng"));
    }

    private Lesson editableLesson(UUID lessonId) {
        Lesson lesson = accessibleLesson(lessonId);
        courseAccess.editableRevision(lesson.getChapter().getRevision());
        return lesson;
    }

    private void requireFreeSlug(Chapter chapter, String slug, UUID lessonId) {
        if (lessonRepository.isSlugTaken(chapter.getRevision().getId(), slug, lessonId)) {
            throw new ConflictException(
                    ErrorCode.SLUG_TAKEN, "Slug đã được bài giảng khác trong khoá dùng");
        }
    }
}
