package com.sqljudge.contest.standings;

import com.sqljudge.contest.entity.Contest;
import com.sqljudge.contest.entity.ContestCell;
import com.sqljudge.contest.entity.ContestProblem;
import com.sqljudge.contest.entity.ContestRegistration;
import com.sqljudge.contest.standings.StandingRow.StandingCell;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class StandingRows {

    private StandingRows() {}

    public static StandingRow publicRow(
            Contest contest, ContestRegistration registration, List<ContestCell> cells) {
        return build(
                contest,
                registration,
                cells,
                cell ->
                        new StandingCell(
                                cell.getProblemId(),
                                cell.getPublicAcceptedMinute(),
                                cell.getPublicWrongAttempts(),
                                cell.getPendingAttempts()),
                ContestCell::getPublicPenaltyMinutes);
    }

    public static StandingRow actualRow(
            Contest contest, ContestRegistration registration, List<ContestCell> cells) {
        return build(
                contest,
                registration,
                cells,
                cell ->
                        new StandingCell(
                                cell.getProblemId(),
                                cell.getAcceptedMinute(),
                                cell.getWrongAttempts(),
                                0),
                ContestCell::getPenaltyMinutes);
    }

    private static StandingRow build(
            Contest contest,
            ContestRegistration registration,
            List<ContestCell> cells,
            Function<ContestCell, StandingCell> view,
            Function<ContestCell, Integer> penaltyOf) {
        Map<UUID, ContestCell> byProblem =
                cells.stream()
                        .collect(Collectors.toMap(ContestCell::getProblemId, Function.identity()));
        List<StandingCell> ordered =
                contest.getProblems().stream()
                        .map(ContestProblem::getProblemId)
                        .map(
                                problemId -> {
                                    ContestCell cell = byProblem.get(problemId);
                                    return cell == null
                                            ? new StandingCell(problemId, null, 0, 0)
                                            : view.apply(cell);
                                })
                        .toList();
        int solved = (int) ordered.stream().filter(cell -> cell.acceptedMinute() != null).count();
        long penalty =
                contest.getProblems().stream()
                        .map(problem -> byProblem.get(problem.getProblemId()))
                        .filter(Objects::nonNull)
                        .mapToLong(penaltyOf::apply)
                        .sum();
        return new StandingRow(
                registration.getUserId(),
                registration.getUsername(),
                registration.getDisplayName(),
                solved,
                penalty,
                ordered);
    }
}
