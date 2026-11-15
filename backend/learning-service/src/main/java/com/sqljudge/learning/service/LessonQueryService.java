package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.learning.dto.response.LessonResponse;
import com.sqljudge.learning.repository.LessonCellRepository;
import com.sqljudge.learning.repository.LessonRepository;
import com.sqljudge.learning.repository.PublishedLessonRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonQueryService {

    private final LessonRepository lessonRepository;
    private final LessonCellRepository lessonCellRepository;
    private final QuizService quizService;

    @Transactional(readOnly = true)
    public LessonResponse getPublished(String courseSlug, String lessonSlug) {
        PublishedLessonRow lesson =
                lessonRepository
                        .findPublished(courseSlug, lessonSlug)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.LESSON_NOT_FOUND,
                                                "Không tìm thấy bài giảng"));
        return new LessonResponse(
                lesson.lessonKey(),
                lesson.slug(),
                lesson.title(),
                lesson.body(),
                lesson.chapterTitle(),
                lesson.courseSlug(),
                lesson.courseTitle(),
                lessonCellRepository.findByLessonId(lesson.lessonId()),
                quizService.questionsOf(lesson.lessonId()));
    }
}
