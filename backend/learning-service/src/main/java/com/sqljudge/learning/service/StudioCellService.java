package com.sqljudge.learning.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.learning.dto.request.CellRequest;
import com.sqljudge.learning.dto.response.StudioCellResponse;
import com.sqljudge.learning.entity.Lesson;
import com.sqljudge.learning.entity.LessonCell;
import com.sqljudge.learning.repository.LessonCellRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class StudioCellService {

    private final LessonCellRepository lessonCellRepository;
    private final StudioCourseAccess courseAccess;
    private final LessonDatasetResolver datasetResolver;
    private final TransactionTemplate transactionTemplate;

    public List<StudioCellResponse> list(UUID lessonId) {
        return transactionTemplate.execute(
                status -> {
                    courseAccess.accessibleLesson(lessonId);
                    return lessonCellRepository.findStudioCells(lessonId).stream()
                            .map(StudioCellResponse::of)
                            .toList();
                });
    }

    public StudioCellResponse add(UUID lessonId, CellRequest request) {
        transactionTemplate.executeWithoutResult(status -> courseAccess.editableLesson(lessonId));
        LessonDataset dataset = datasetResolver.resolve(request.datasetVariantId());
        return transactionTemplate.execute(
                status -> {
                    Lesson lesson = courseAccess.editableLesson(lessonId);
                    LessonCell cell =
                            LessonCell.of(
                                    lesson, lessonCellRepository.findLastPosition(lessonId) + 1);
                    return StudioCellResponse.of(
                            lessonCellRepository.save(writeInto(cell, request, dataset)));
                });
    }

    public StudioCellResponse update(UUID cellId, CellRequest request) {
        transactionTemplate.executeWithoutResult(status -> editableCell(cellId));
        LessonDataset dataset = datasetResolver.resolve(request.datasetVariantId());
        return transactionTemplate.execute(
                status ->
                        StudioCellResponse.of(
                                lessonCellRepository.save(
                                        writeInto(editableCell(cellId), request, dataset))));
    }

    public void delete(UUID cellId) {
        transactionTemplate.executeWithoutResult(
                status -> lessonCellRepository.delete(editableCell(cellId)));
    }

    private LessonCell editableCell(UUID cellId) {
        LessonCell cell =
                lessonCellRepository
                        .findWithCourse(cellId)
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
                                                ErrorCode.LESSON_CELL_NOT_FOUND,
                                                "Không tìm thấy ô SQL"));
        courseAccess.editableRevision(cell.getLesson().getChapter().getRevision());
        return cell;
    }

    private static LessonCell writeInto(
            LessonCell cell, CellRequest request, LessonDataset dataset) {
        cell.configure(request.starterSql(), request.dialect());
        cell.useDataset(dataset.variantId(), dataset.versionId(), dataset.checksum());
        return cell;
    }
}
