package com.sqljudge.notification.dto.response;

import java.util.List;

public record NotificationFeedResponse(List<NotificationResponse> items, String nextCursor) {

    public NotificationFeedResponse {
        items = List.copyOf(items);
    }
}
