package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.response.SolvedSummaryResponse;
import com.sqljudge.problem.dto.response.SolvedSummaryResponse.SolvedProgress;
import com.sqljudge.problem.dto.response.TopicProgressResponse;
import com.sqljudge.problem.dto.response.TopicProgressResponse.TopicProgress;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.ProblemProgressStatus;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.repository.DifficultyCount;
import com.sqljudge.problem.repository.ProblemRepository;
import com.sqljudge.problem.repository.TagUsageRow;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolvedSummaryService {

    private static final Comparator<TopicProgress> BY_SIZE_THEN_NAME =
            Comparator.comparingLong(TopicProgress::total)
                    .reversed()
                    .thenComparing(TopicProgress::name);

    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public SolvedSummaryResponse summary(UUID userId) {
        Map<Difficulty, Long> totals =
                countsOf(
                        problemRepository.countPublishedByDifficulty(
                                ProblemStatus.PUBLISHED, ProblemVisibility.PUBLIC));
        Map<Difficulty, Long> solved =
                countsOf(
                        problemRepository.countSolvedByDifficulty(
                                userId, ProblemProgressStatus.SOLVED));
        Map<Difficulty, SolvedProgress> byDifficulty =
                Arrays.stream(Difficulty.values())
                        .collect(
                                Collectors.toMap(
                                        Function.identity(),
                                        difficulty ->
                                                new SolvedProgress(
                                                        solved.getOrDefault(difficulty, 0L),
                                                        totals.getOrDefault(difficulty, 0L))));
        long solvedCount = solved.values().stream().mapToLong(Long::longValue).sum();
        return new SolvedSummaryResponse(userId, solvedCount, byDifficulty);
    }

    @Transactional(readOnly = true)
    public TopicProgressResponse topics(UUID userId) {
        Map<String, Long> solved =
                problemRepository
                        .countSolvedByTag(
                                userId,
                                ProblemProgressStatus.SOLVED,
                                ProblemStatus.PUBLISHED,
                                ProblemVisibility.PUBLIC)
                        .stream()
                        .collect(Collectors.toMap(TagUsageRow::getSlug, TagUsageRow::getTotal));
        List<TopicProgress> topics =
                problemRepository
                        .countPublishedByTag(ProblemStatus.PUBLISHED, ProblemVisibility.PUBLIC)
                        .stream()
                        .map(
                                tag ->
                                        new TopicProgress(
                                                tag.getSlug(),
                                                tag.getName(),
                                                solved.getOrDefault(tag.getSlug(), 0L),
                                                tag.getTotal()))
                        .sorted(BY_SIZE_THEN_NAME)
                        .toList();
        return new TopicProgressResponse(userId, topics);
    }

    private static Map<Difficulty, Long> countsOf(List<DifficultyCount> counts) {
        return counts.stream()
                .collect(
                        Collectors.toMap(
                                DifficultyCount::getDifficulty, DifficultyCount::getTotal));
    }
}
