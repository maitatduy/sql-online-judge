package com.sqljudge.contest.service;

import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestAttempt;
import com.sqljudge.contest.entity.ContestCell;
import com.sqljudge.contest.entity.ContestCellKey;
import com.sqljudge.contest.entity.ContestProblem;
import com.sqljudge.contest.event.SubmissionJudgedEvent;
import com.sqljudge.contest.repository.ContestAttemptRepository;
import com.sqljudge.contest.repository.ContestCellRepository;
import com.sqljudge.contest.repository.ContestRepository;
import com.sqljudge.contest.scoring.Attempt;
import com.sqljudge.contest.scoring.IcpcScoring;
import com.sqljudge.contest.scoring.JudgeVerdict;
import com.sqljudge.contest.scoring.ScoringRules;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestAttemptService {

    private final ContestRepository contestRepository;
    private final ContestAttemptRepository attemptRepository;
    private final ContestCellRepository cellRepository;
    private final StandingsService standingsService;

    @Transactional
    public void record(SubmissionJudgedEvent event) {
        if (event.contestId() == null
                || JudgeVerdict.SE.name().equals(event.verdict())
                || attemptRepository.existsBySubmissionId(event.submissionId())) {
            return;
        }
        Contest contest = contestRepository.findById(event.contestId()).orElse(null);
        if (contest == null || !includesProblem(contest, event)) {
            log.warn(
                    "contest_attempt_ignored contestId={} submissionId={}",
                    event.contestId(),
                    event.submissionId());
            return;
        }
        ContestCellKey key = new ContestCellKey(contest.getId(), event.userId(), event.problemId());
        attemptRepository.saveAndFlush(
                ContestAttempt.of(
                        key,
                        event.submissionId(),
                        new Attempt(event.submittedAt(), JudgeVerdict.valueOf(event.verdict()))));
        rescore(key, contest.scoringRules());
        standingsService.refreshAfterCommit(contest, event.userId());
    }

    private void rescore(ContestCellKey key, ScoringRules rules) {
        List<Attempt> attempts =
                attemptRepository
                        .findByContestIdAndUserIdAndProblemId(
                                key.contestId(), key.userId(), key.problemId())
                        .stream()
                        .map(ContestAttempt::toAttempt)
                        .toList();
        ContestCell cell =
                cellRepository
                        .findByContestIdAndUserIdAndProblemId(
                                key.contestId(), key.userId(), key.problemId())
                        .orElseGet(() -> ContestCell.empty(key));
        cell.update(IcpcScoring.score(attempts, rules), IcpcScoring.publicScore(attempts, rules));
        cellRepository.saveAndFlush(cell);
        log.info(
                "contest_cell_scored contestId={} userId={} problemId={}",
                key.contestId(),
                key.userId(),
                key.problemId());
    }

    private static boolean includesProblem(Contest contest, SubmissionJudgedEvent event) {
        return contest.getProblems().stream()
                .map(ContestProblem::getProblemId)
                .anyMatch(problemId -> problemId.equals(event.problemId()));
    }
}
