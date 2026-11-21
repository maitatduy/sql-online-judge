package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.learning.dto.response.StudioCourseResponse;
import com.sqljudge.learning.entity.Chapter;
import com.sqljudge.learning.entity.Course;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.entity.Lesson;
import com.sqljudge.learning.entity.QuizQuestion;
import com.sqljudge.learning.repository.ChapterRepository;
import com.sqljudge.learning.repository.CourseRevisionRepository;
import com.sqljudge.learning.repository.LessonCellRepository;
import com.sqljudge.learning.repository.LessonRepository;
import com.sqljudge.learning.repository.QuizOptionRepository;
import com.sqljudge.learning.repository.QuizQuestionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudioCourseDraftService {

    private final CourseRevisionRepository courseRevisionRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final LessonCellRepository lessonCellRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final StudioCourseAccess courseAccess;

    @Transactional
    public StudioCourseResponse createDraft(UUID courseId) {
        Course course = courseAccess.accessible(courseId);
        CourseRevision published =
                Optional.ofNullable(course.getPublishedRevisionId())
                        .flatMap(courseRevisionRepository::findById)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                ErrorCode.COURSE_NOT_PUBLISHED,
                                                "Khoá chưa xuất bản nên chưa tạo bản sửa được"));
        if (course.getDraftRevisionId() != null) {
            throw new ConflictException(
                    ErrorCode.COURSE_DRAFT_EXISTS, "Khoá đã có bản nháp đang soạn");
        }
        CourseRevision draft = courseRevisionRepository.saveAndFlush(published.copyAsDraft());
        Map<UUID, Lesson> copiedLessons = copyLessons(published, draft);
        if (!copiedLessons.isEmpty()) {
            copyCells(copiedLessons);
            copyQuiz(copiedLessons);
        }
        course.setDraftRevisionId(draft.getId());
        log.info("course_draft_created courseId={} revisionId={}", course.getId(), draft.getId());
        return StudioCourseResponse.of(course, draft);
    }

    private Map<UUID, Lesson> copyLessons(CourseRevision source, CourseRevision target) {
        Map<UUID, Chapter> chapters = new HashMap<>();
        for (Chapter chapter : chapterRepository.findByRevisionIdOrderByPosition(source.getId())) {
            chapters.put(
                    chapter.getId(),
                    chapterRepository.save(
                            Chapter.of(target, chapter.getTitle(), chapter.getPosition())));
        }
        Map<UUID, Lesson> lessons = new HashMap<>();
        for (Lesson lesson : lessonRepository.findByRevision(source.getId())) {
            lessons.put(
                    lesson.getId(),
                    lessonRepository.save(
                            lesson.copyTo(chapters.get(lesson.getChapter().getId()))));
        }
        return lessons;
    }

    private void copyCells(Map<UUID, Lesson> copiedLessons) {
        lessonCellRepository.saveAll(
                lessonCellRepository.findByLessonIds(copiedLessons.keySet()).stream()
                        .map(cell -> cell.copyTo(copiedLessons.get(cell.getLesson().getId())))
                        .toList());
    }

    private void copyQuiz(Map<UUID, Lesson> copiedLessons) {
        List<QuizQuestion> questions =
                quizQuestionRepository.findByLessonIds(copiedLessons.keySet());
        Map<UUID, QuizQuestion> copiedQuestions = new HashMap<>();
        for (QuizQuestion question : questions) {
            copiedQuestions.put(
                    question.getId(),
                    quizQuestionRepository.save(
                            question.copyTo(copiedLessons.get(question.getLesson().getId()))));
        }
        if (copiedQuestions.isEmpty()) {
            return;
        }
        quizOptionRepository.saveAll(
                quizOptionRepository.findByQuestionIds(copiedQuestions.keySet()).stream()
                        .map(
                                option ->
                                        option.copyTo(
                                                copiedQuestions.get(option.getQuestion().getId())))
                        .toList());
    }
}
