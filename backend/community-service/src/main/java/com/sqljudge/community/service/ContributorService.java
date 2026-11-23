package com.sqljudge.community.service;

import com.sqljudge.community.dto.response.ContributorResponse;
import com.sqljudge.community.repository.ContributorRow;
import com.sqljudge.community.repository.VoteRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContributorService {

    private static final Duration WINDOW = Duration.ofDays(7);
    private static final int TOP = 5;

    private final VoteRepository voteRepository;
    private final AuthorNames authorNames;
    private final Clock clock;

    public List<ContributorResponse> topThisWeek() {
        List<ContributorRow> rows = rows();
        Map<UUID, String> names =
                authorNames.of(rows.stream().map(ContributorRow::getUserId).toList());
        return rows.stream()
                .map(
                        row ->
                                new ContributorResponse(
                                        row.getUserId(),
                                        names.get(row.getUserId()),
                                        row.getPoints()))
                .toList();
    }

    private List<ContributorRow> rows() {
        return voteRepository.findTopContributors(
                clock.instant().minus(WINDOW), PageRequest.of(0, TOP));
    }
}
