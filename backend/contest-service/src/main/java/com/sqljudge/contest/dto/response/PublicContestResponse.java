package com.sqljudge.contest.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public record PublicContestResponse(@JsonUnwrapped ContestResponse contest, long registrantCount) {}
