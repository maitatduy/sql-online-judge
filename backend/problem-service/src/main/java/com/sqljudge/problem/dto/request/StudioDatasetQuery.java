package com.sqljudge.problem.dto.request;

public record StudioDatasetQuery(
        DatasetVersionFilter status, String q, String sort, int page, int size) {}
