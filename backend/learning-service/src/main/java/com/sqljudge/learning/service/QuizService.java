package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.learning.dto.request.SubmitQuizRequest;
import com.sqljudge.learning.dto.request.SubmitQuizRequest.Answer;
import com.sqljudge.learning.dto.response.CourseProgressResponse;
import com.sqljudge.learning.dto.response.QuizQuestionResponse;
import com.sqljudge.learning.dto.response.QuizResultResponse;
import com.sqljudge.learning.dto.response.QuizResultResponse.QuestionResult;
import com.sqljudge.learning.repository.LessonRepository;
import com.sqljudge.learning.repository.PublishedLessonRow;
import com.sqljudge.learning.repository.QuizOptionRepository;
import com.sqljudge.learning.repository.QuizOptionRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final LessonRepository lessonRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final LearningProgressService learningProgressService;

    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> questionsOf(UUID lessonId) {
        Map<UUID, QuizQuestionResponse> questions = new LinkedHashMap<>();
        Map<UUID, List<QuizQuestionResponse.Option>> options = new HashMap<>();
        for (QuizOptionRow row : quizOptionRepository.findByLessonId(lessonId)) {
            List<QuizQuestionResponse.Option> questionOptions =
                    options.computeIfAbsent(row.questionKey(), key -> new ArrayList<>());
            questionOptions.add(new QuizQuestionResponse.Option(row.optionKey(), row.label()));
            questions.putIfAbsent(
                    row.questionKey(),
                    new QuizQuestionResponse(
                            row.questionKey(), row.prompt(), row.multiple(), List.of()));
        }
        return questions.values().stream()
                .map(
                        question ->
                                new QuizQuestionResponse(
                                        question.questionKey(),
                                        question.prompt(),
                                        question.multiple(),
                                        options.get(question.questionKey())))
                .toList();
    }

    @Transactional
    public QuizResultResponse grade(
            String courseSlug, String lessonSlug, SubmitQuizRequest request) {
        PublishedLessonRow lesson =
                lessonRepository
                        .findPublished(courseSlug, lessonSlug)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.LESSON_NOT_FOUND,
                                                "Không tìm thấy bài giảng"));
        Map<UUID, Set<UUID>> correctOptions = correctOptionsOf(lesson.lessonId());
        if (correctOptions.isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCode.LESSON_HAS_NO_QUIZ, "Bài giảng này không có câu hỏi");
        }
        Map<UUID, Set<UUID>> chosenOptions = chosenOptionsOf(request.answers());
        List<QuestionResult> results =
                correctOptions.entrySet().stream()
                        .map(
                                question ->
                                        new QuestionResult(
                                                question.getKey(),
                                                question.getValue()
                                                        .equals(
                                                                chosenOptions.get(
                                                                        question.getKey()))))
                        .toList();
        boolean allCorrect = results.stream().allMatch(QuestionResult::correct);
        CourseProgressResponse progress =
                allCorrect
                        ? learningProgressService.recordCompletion(courseSlug, lessonSlug)
                        : learningProgressService.progress(courseSlug);
        return new QuizResultResponse(allCorrect, results, progress);
    }

    private Map<UUID, Set<UUID>> correctOptionsOf(UUID lessonId) {
        Map<UUID, Set<UUID>> correctOptions = new LinkedHashMap<>();
        for (QuizOptionRow row : quizOptionRepository.findByLessonId(lessonId)) {
            Set<UUID> options =
                    correctOptions.computeIfAbsent(row.questionKey(), key -> new HashSet<>());
            if (row.correct()) {
                options.add(row.optionKey());
            }
        }
        return correctOptions;
    }

    private static Map<UUID, Set<UUID>> chosenOptionsOf(List<Answer> answers) {
        Map<UUID, Set<UUID>> chosen = new HashMap<>();
        for (Answer answer : answers) {
            chosen.put(answer.questionKey(), new HashSet<>(answer.optionKeys()));
        }
        return chosen;
    }
}
