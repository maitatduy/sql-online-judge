package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.learning.dto.request.QuizOptionRequest;
import com.sqljudge.learning.dto.request.QuizQuestionRequest;
import com.sqljudge.learning.dto.response.StudioQuizQuestionResponse;
import com.sqljudge.learning.entity.QuizOption;
import com.sqljudge.learning.entity.QuizQuestion;
import com.sqljudge.learning.repository.QuizOptionRepository;
import com.sqljudge.learning.repository.QuizQuestionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioQuizService {

    private static final String OPTIONS_FIELD = "options";

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final StudioCourseAccess courseAccess;

    @Transactional(readOnly = true)
    public List<StudioQuizQuestionResponse> list(UUID lessonId) {
        courseAccess.accessibleLesson(lessonId);
        List<QuizQuestion> questions = quizQuestionRepository.findStudioQuestions(lessonId);
        Map<UUID, List<QuizOption>> options =
                quizOptionRepository
                        .findByQuestionIds(questions.stream().map(QuizQuestion::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));
        return questions.stream()
                .map(
                        question ->
                                StudioQuizQuestionResponse.of(
                                        question,
                                        options.getOrDefault(question.getId(), List.of())))
                .toList();
    }

    @Transactional
    public StudioQuizQuestionResponse add(UUID lessonId, QuizQuestionRequest request) {
        requireValidAnswers(request);
        QuizQuestion question =
                QuizQuestion.of(
                        courseAccess.editableLesson(lessonId),
                        quizQuestionRepository.findLastPosition(lessonId) + 1);
        question.ask(request.prompt(), request.multiple());
        return saveWithOptions(question, request.options());
    }

    @Transactional
    public StudioQuizQuestionResponse replace(UUID questionId, QuizQuestionRequest request) {
        requireValidAnswers(request);
        QuizQuestion question = editableQuestion(questionId);
        question.ask(request.prompt(), request.multiple());
        quizOptionRepository.deleteByQuestionId(questionId);
        return saveWithOptions(question, request.options());
    }

    @Transactional
    public void delete(UUID questionId) {
        QuizQuestion question = editableQuestion(questionId);
        quizOptionRepository.deleteByQuestionId(questionId);
        quizQuestionRepository.delete(question);
    }

    private StudioQuizQuestionResponse saveWithOptions(
            QuizQuestion question, List<QuizOptionRequest> requests) {
        QuizQuestion saved = quizQuestionRepository.save(question);
        List<QuizOption> options =
                IntStream.range(0, requests.size())
                        .mapToObj(index -> optionOf(saved, index + 1, requests.get(index)))
                        .toList();
        return StudioQuizQuestionResponse.of(saved, quizOptionRepository.saveAll(options));
    }

    private static QuizOption optionOf(
            QuizQuestion question, int position, QuizOptionRequest request) {
        QuizOption option = QuizOption.of(question, position);
        option.describe(request.label(), request.correct());
        return option;
    }

    private QuizQuestion editableQuestion(UUID questionId) {
        QuizQuestion question =
                quizQuestionRepository
                        .findWithCourse(questionId)
                        .filter(
                                found ->
                                        StudioCourseAccess.isMine(
                                                found.getLesson()
                                                        .getChapter()
                                                        .getRevision()
                                                        .getCourse()))
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.QUIZ_QUESTION_NOT_FOUND,
                                                "Không tìm thấy câu hỏi"));
        courseAccess.editableRevision(question.getLesson().getChapter().getRevision());
        return question;
    }

    private static void requireValidAnswers(QuizQuestionRequest request) {
        long correctCount = request.correctCount();
        if (correctCount == 0) {
            throw new InvalidFieldException(OPTIONS_FIELD, "Cần ít nhất một đáp án đúng");
        }
        if (!request.multiple() && correctCount > 1) {
            throw new InvalidFieldException(
                    OPTIONS_FIELD, "Câu hỏi một đáp án chỉ có một lựa chọn đúng");
        }
    }
}
