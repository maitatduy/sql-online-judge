package com.sqljudge.learning.dto.response;

import java.time.LocalDate;

public record ActivityDayResponse(LocalDate date, long count) {}
