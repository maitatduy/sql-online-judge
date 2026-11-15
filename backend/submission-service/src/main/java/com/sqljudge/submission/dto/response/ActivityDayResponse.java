package com.sqljudge.submission.dto.response;

import java.time.LocalDate;

public record ActivityDayResponse(LocalDate date, long count) {}
