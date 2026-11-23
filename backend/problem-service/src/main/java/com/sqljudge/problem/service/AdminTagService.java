package com.sqljudge.problem.service;

import com.sqljudge.problem.dto.response.AdminTagResponse;
import com.sqljudge.problem.repository.TagRepository;
import com.sqljudge.problem.repository.TagUsageRow;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTagService {

    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<AdminTagResponse> list() {
        Map<String, Long> counts =
                tagRepository.countLiveProblemsPerTag().stream()
                        .collect(Collectors.toMap(TagUsageRow::getSlug, TagUsageRow::getTotal));
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(
                        tag ->
                                new AdminTagResponse(
                                        tag.getSlug(),
                                        tag.getName(),
                                        counts.getOrDefault(tag.getSlug(), 0L)))
                .toList();
    }
}
