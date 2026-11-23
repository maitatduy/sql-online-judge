package com.sqljudge.contest.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class StandingsStreamService {

    private static final String EVENT_NAME = "standings";

    private final Map<UUID, Set<SseEmitter>> viewersByContest = new ConcurrentHashMap<>();
    private final Set<UUID> changedContests = ConcurrentHashMap.newKeySet();

    public void register(UUID contestId, SseEmitter emitter) {
        viewersByContest
                .computeIfAbsent(contestId, id -> ConcurrentHashMap.newKeySet())
                .add(emitter);
        Runnable unregister = () -> unregister(contestId, emitter);
        emitter.onCompletion(unregister);
        emitter.onTimeout(unregister);
        emitter.onError(error -> unregister.run());
    }

    public void markChanged(UUID contestId) {
        changedContests.add(contestId);
    }

    public void flush() {
        for (UUID contestId : Set.copyOf(changedContests)) {
            changedContests.remove(contestId);
            Set<SseEmitter> viewers = viewersByContest.get(contestId);
            if (viewers != null) {
                Set.copyOf(viewers).forEach(viewer -> notify(contestId, viewer));
            }
        }
    }

    public int viewerCount(UUID contestId) {
        Set<SseEmitter> viewers = viewersByContest.get(contestId);
        return viewers == null ? 0 : viewers.size();
    }

    private void notify(UUID contestId, SseEmitter viewer) {
        try {
            viewer.send(SseEmitter.event().name(EVENT_NAME).data(contestId.toString()));
        } catch (IOException | IllegalStateException closed) {
            log.debug("standings_stream_closed contestId={}", contestId);
            unregister(contestId, viewer);
        }
    }

    private void unregister(UUID contestId, SseEmitter viewer) {
        viewersByContest.computeIfPresent(
                contestId,
                (id, viewers) -> {
                    viewers.remove(viewer);
                    return viewers.isEmpty() ? null : viewers;
                });
    }
}
