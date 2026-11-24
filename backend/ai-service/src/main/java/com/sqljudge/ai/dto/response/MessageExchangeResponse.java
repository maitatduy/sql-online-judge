package com.sqljudge.ai.dto.response;

public record MessageExchangeResponse(
        MessageResponse question, MessageResponse answer, QuotaResponse quota) {}
