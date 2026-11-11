package com.sqljudge.submission.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionChangeBroadcaster {

    private final StringRedisTemplate redisTemplate;

    @TransactionalEventListener
    void broadcast(SubmissionChanged change) {
        try {
            redisTemplate.convertAndSend(
                    SubmissionChanged.CHANNEL, change.submissionId().toString());
        } catch (DataAccessException exception) {
            log.warn(
                    "submission_change_broadcast_failed submissionId={}",
                    change.submissionId(),
                    exception);
        }
    }
}
