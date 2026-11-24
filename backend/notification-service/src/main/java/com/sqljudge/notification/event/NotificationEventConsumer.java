package com.sqljudge.notification.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.notification.event.IncomingEvents.Commented;
import com.sqljudge.notification.event.IncomingEvents.ProblemPublished;
import com.sqljudge.notification.event.IncomingEvents.RatingUpdated;
import com.sqljudge.notification.event.IncomingEvents.ReportResolved;
import com.sqljudge.notification.service.NotificationDrafts;
import com.sqljudge.notification.service.NotificationInbox;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationInbox inbox;

    @KafkaListener(topics = NotificationTopics.COMMENTED)
    public void onCommented(String payload) throws JsonProcessingException {
        inbox.deliver(NotificationDrafts.of(objectMapper.readValue(payload, Commented.class)));
    }

    @KafkaListener(topics = NotificationTopics.REPORT_RESOLVED)
    public void onReportResolved(String payload) throws JsonProcessingException {
        inbox.deliver(NotificationDrafts.of(objectMapper.readValue(payload, ReportResolved.class)));
    }

    @KafkaListener(topics = NotificationTopics.PROBLEM_PUBLISHED)
    public void onProblemPublished(String payload) throws JsonProcessingException {
        inbox.deliver(
                NotificationDrafts.of(objectMapper.readValue(payload, ProblemPublished.class)));
    }

    @KafkaListener(topics = NotificationTopics.RATING_UPDATED)
    public void onRatingUpdated(String payload) throws JsonProcessingException {
        inbox.deliver(NotificationDrafts.of(objectMapper.readValue(payload, RatingUpdated.class)));
    }
}
