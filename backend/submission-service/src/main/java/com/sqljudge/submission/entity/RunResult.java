package com.sqljudge.submission.entity;

import java.util.List;

public record RunResult(List<String> columns, List<List<String>> rows, boolean isTruncated) {}
