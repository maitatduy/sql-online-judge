package com.sqljudge.submission.service;

import com.sqljudge.submission.dto.response.SubmissionResponse;
import com.sqljudge.submission.entity.SubmissionStatus;
import com.sqljudge.submission.repository.SubmissionRepository;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionStreamService {

    private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(5);
    private static final String EVENT_NAME = "submission";

    private final SubmissionQueryService submissionQueryService;
    private final SubmissionRepository submissionRepository;
    private final Map<UUID, Set<SseEmitter>> emittersBySubmission = new ConcurrentHashMap<>();

    public SseEmitter open(UUID submissionId) {
        UUID ownedId = submissionQueryService.get(submissionId).id();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT.toMillis());
        register(ownedId, emitter);
        send(ownedId, emitter, submissionQueryService.get(ownedId));
        return emitter;
    }

    public void onChanged(UUID submissionId) {
        Set<SseEmitter> emitters = emittersBySubmission.get(submissionId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        submissionRepository
                .findById(submissionId)
                .map(SubmissionResponse::from)
                .ifPresent(
                        response ->
                                emitters.forEach(emitter -> send(submissionId, emitter, response)));
    }

    private void register(UUID submissionId, SseEmitter emitter) {
        emittersBySubmission
                .computeIfAbsent(submissionId, id -> ConcurrentHashMap.newKeySet())
                .add(emitter);
        Runnable unregister = () -> unregister(submissionId, emitter);
        emitter.onCompletion(unregister);
        emitter.onTimeout(unregister);
        emitter.onError(error -> unregister.run());
    }

    private void unregister(UUID submissionId, SseEmitter emitter) {
        emittersBySubmission.computeIfPresent(
                submissionId,
                (id, emitters) -> {
                    emitters.remove(emitter);
                    return emitters.isEmpty() ? null : emitters;
                });
    }

    private void send(UUID submissionId, SseEmitter emitter, SubmissionResponse response) {
        try {
            emitter.send(SseEmitter.event().name(EVENT_NAME).data(response));
        } catch (IOException | IllegalStateException exception) {
            log.debug("submission_stream_closed submissionId={}", submissionId);
            unregister(submissionId, emitter);
            return;
        }
        if (response.status() == SubmissionStatus.COMPLETED) {
            unregister(submissionId, emitter);
            emitter.complete();
        }
    }
}
