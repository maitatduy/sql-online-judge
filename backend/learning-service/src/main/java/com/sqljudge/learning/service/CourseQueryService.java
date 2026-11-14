package com.sqljudge.learning.service;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.learning.dto.response.CourseSummaryResponse;
import com.sqljudge.learning.entity.Difficulty;
import com.sqljudge.learning.repository.CourseRepository;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseQueryService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> listPublished(
            Difficulty difficulty, int page, int size) {
        return PageResponse.from(
                courseRepository.findPublished(difficulty, PageRequest.of(page - 1, size)),
                Function.identity());
    }
}
