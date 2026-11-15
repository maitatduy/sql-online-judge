package com.sqljudge.learning.service;

import com.sqljudge.common.exception.InvalidFieldException;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.learning.dto.response.ActivityDayResponse;
import com.sqljudge.learning.dto.response.CourseProgressResponse;
import com.sqljudge.learning.dto.response.EnrolledCourseResponse;
import com.sqljudge.learning.entity.CourseRevision;
import com.sqljudge.learning.entity.Enrollment;
import com.sqljudge.learning.repository.CourseRevisionRepository;
import com.sqljudge.learning.repository.EnrollmentRepository;
import com.sqljudge.learning.repository.LessonProgressRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningDashboardService {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DEFAULT_ACTIVITY_DAYS = 364;
    private static final int MAX_ACTIVITY_DAYS = 366;

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRevisionRepository courseRevisionRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LearningProgressService learningProgressService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<EnrolledCourseResponse> enrollments() {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        return enrollmentRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Enrollment::getCourseId)
                .map(courseRevisionRepository::findPublishedByCourseId)
                .flatMap(Optional::stream)
                .map(revision -> enrolledCourseOf(revision, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityDayResponse> activity(LocalDate from) {
        LocalDate today = LocalDate.now(clock.withZone(VIETNAM));
        LocalDate start = from == null ? today.minusDays(DEFAULT_ACTIVITY_DAYS) : from;
        if (start.isBefore(today.minusDays(MAX_ACTIVITY_DAYS))) {
            throw new InvalidFieldException(
                    "from", "Chỉ xem được hoạt động trong một năm gần nhất");
        }
        UUID userId = SecurityUtils.requireCurrentUser().id();
        Map<LocalDate, Long> counts =
                lessonProgressRepository
                        .findCompletedAtSince(userId, start.atStartOfDay(VIETNAM).toInstant())
                        .stream()
                        .map(completedAt -> LocalDate.ofInstant(completedAt, VIETNAM))
                        .collect(
                                Collectors.groupingBy(
                                        Function.identity(), TreeMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(day -> new ActivityDayResponse(day.getKey(), day.getValue()))
                .toList();
    }

    private EnrolledCourseResponse enrolledCourseOf(CourseRevision revision, UUID userId) {
        CourseProgressResponse progress = learningProgressService.progressOf(revision, userId);
        return new EnrolledCourseResponse(
                revision.getCourse().getSlug(),
                revision.getTitle(),
                revision.getDifficulty(),
                progress.completedLessonKeys().size(),
                progress.lessonCount(),
                progress.nextLessonSlug());
    }
}
