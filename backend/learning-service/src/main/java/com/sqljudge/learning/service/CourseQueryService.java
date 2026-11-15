package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.learning.dto.response.CourseDetailResponse;
import com.sqljudge.learning.dto.response.CourseDetailResponse.ChapterOutline;
import com.sqljudge.learning.dto.response.CourseDetailResponse.LessonOutline;
import com.sqljudge.learning.dto.response.CourseSummaryResponse;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.entity.Difficulty;
import com.sqljudge.learning.repository.CourseRepository;
import com.sqljudge.learning.repository.CourseRevisionRepository;
import com.sqljudge.learning.repository.LessonOutlineRow;
import com.sqljudge.learning.repository.LessonRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final CourseRevisionRepository courseRevisionRepository;
    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public PageResponse<CourseSummaryResponse> listPublished(
            Difficulty difficulty, int page, int size) {
        return PageResponse.from(
                courseRepository.findPublished(difficulty, PageRequest.of(page - 1, size)),
                Function.identity());
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getPublished(String slug) {
        CourseRevision revision =
                courseRevisionRepository
                        .findPublishedBySlug(slug)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                ErrorCode.COURSE_NOT_FOUND,
                                                "Không tìm thấy khoá học"));
        return new CourseDetailResponse(
                revision.getCourse().getId(),
                revision.getCourse().getSlug(),
                revision.getTitle(),
                revision.getSummary(),
                revision.getDifficulty(),
                revision.getLessonCount(),
                revision.getEstimatedMinutes(),
                revision.getPublishedAt(),
                outline(lessonRepository.findOutline(revision.getId())));
    }

    private static List<ChapterOutline> outline(List<LessonOutlineRow> rows) {
        Map<UUID, String> titles = new LinkedHashMap<>();
        Map<UUID, List<LessonOutline>> lessons = new LinkedHashMap<>();
        for (LessonOutlineRow row : rows) {
            titles.putIfAbsent(row.chapterId(), row.chapterTitle());
            List<LessonOutline> chapterLessons =
                    lessons.computeIfAbsent(row.chapterId(), id -> new ArrayList<>());
            if (row.lessonKey() != null) {
                chapterLessons.add(new LessonOutline(row.lessonKey(), row.slug(), row.title()));
            }
        }
        return titles.entrySet().stream()
                .map(
                        chapter ->
                                new ChapterOutline(
                                        chapter.getKey(),
                                        chapter.getValue(),
                                        lessons.get(chapter.getKey())))
                .toList();
    }
}
