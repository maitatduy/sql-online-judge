package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.learning.dto.request.ChapterRequest;
import com.sqljudge.learning.dto.response.StudioChapterResponse;
import com.sqljudge.learning.dto.response.StudioChapterResponse.LessonOutline;
import com.sqljudge.learning.entity.Chapter;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.repository.ChapterRepository;
import com.sqljudge.learning.repository.LessonRepository;
import com.sqljudge.learning.repository.StudioLessonRow;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudioChapterService {

    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;
    private final StudioCourseAccess courseAccess;

    @Transactional(readOnly = true)
    public List<StudioChapterResponse> list(UUID courseId) {
        CourseRevision revision = courseAccess.workingRevisionOf(courseAccess.accessible(courseId));
        Map<UUID, List<LessonOutline>> lessons =
                lessonRepository.findStudioOutline(revision.getId()).stream()
                        .collect(
                                Collectors.groupingBy(
                                        StudioLessonRow::chapterId,
                                        Collectors.mapping(
                                                row ->
                                                        new LessonOutline(
                                                                row.lessonId(),
                                                                row.slug(),
                                                                row.title()),
                                                Collectors.toList())));
        return chapterRepository.findByRevisionIdOrderByPosition(revision.getId()).stream()
                .map(
                        chapter ->
                                responseOf(
                                        chapter, lessons.getOrDefault(chapter.getId(), List.of())))
                .toList();
    }

    @Transactional
    public StudioChapterResponse add(UUID courseId, ChapterRequest request) {
        CourseRevision revision =
                courseAccess.editableRevision(
                        courseAccess.workingRevisionOf(courseAccess.accessible(courseId)));
        int position = chapterRepository.findLastPosition(revision.getId()) + 1;
        Chapter chapter =
                chapterRepository.saveAndFlush(Chapter.of(revision, request.title(), position));
        return responseOf(chapter, List.of());
    }

    @Transactional
    public StudioChapterResponse rename(UUID chapterId, ChapterRequest request) {
        Chapter chapter = editableChapter(chapterId);
        chapter.setTitle(request.title());
        return responseOf(chapterRepository.saveAndFlush(chapter), List.of());
    }

    @Transactional
    public void delete(UUID chapterId) {
        Chapter chapter = editableChapter(chapterId);
        if (lessonRepository.existsByChapterId(chapterId)) {
            throw new BusinessRuleException(
                    ErrorCode.CHAPTER_NOT_EMPTY, "Chuyển hoặc xoá bài giảng trước khi xoá chương");
        }
        chapterRepository.delete(chapter);
    }

    private Chapter editableChapter(UUID chapterId) {
        Chapter chapter =
                chapterRepository
                        .findWithCourse(chapterId)
                        .filter(found -> StudioCourseAccess.isMine(found.getRevision().getCourse()))
                        .orElseThrow(StudioCourseAccess::courseNotFound);
        courseAccess.editableRevision(chapter.getRevision());
        return chapter;
    }

    private static StudioChapterResponse responseOf(Chapter chapter, List<LessonOutline> lessons) {
        return new StudioChapterResponse(
                chapter.getId(), chapter.getTitle(), chapter.getPosition(), lessons);
    }
}
