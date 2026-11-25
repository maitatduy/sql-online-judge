package com.sqljudge.submission.dto.response;

public record JudgeQueueResponse(
        long waiting,
        long judging,
        long stuck,
        long systemErrorsLastDay,
        long judgedLastHour,
        int stuckAfterMinutes) {}
