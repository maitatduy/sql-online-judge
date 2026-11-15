package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.learning.dto.response.LessonResponse;
import com.sqljudge.learning.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonQueryService {

    private final LessonRepository lessonRepository;

    @Transactional(readOnly = true)
    public LessonResponse getPublished(String courseSlug, String lessonSlug) {
        return lessonRepository
                .findPublished(courseSlug, lessonSlug)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.LESSON_NOT_FOUND, "Không tìm thấy bài giảng"));
    }
}
