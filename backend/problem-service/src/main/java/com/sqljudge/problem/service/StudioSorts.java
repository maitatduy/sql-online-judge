package com.sqljudge.problem.service;

import com.sqljudge.common.exception.InvalidFieldException;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

public final class StudioSorts {

    public static final String NEWEST_FIRST = "updatedAt,desc";
    private static final String UPDATED_AT = "updatedAt";
    private static final String TITLE = "title";
    private static final String NAME = "name";
    private static final Map<String, Sort> PROBLEM_SORTS =
            Map.of(
                    NEWEST_FIRST,
                    Sort.by(Direction.DESC, UPDATED_AT),
                    "updatedAt,asc",
                    Sort.by(Direction.ASC, UPDATED_AT),
                    "title,asc",
                    Sort.by(Direction.ASC, TITLE));
    private static final Map<String, Sort> DATASET_SORTS =
            Map.of(
                    NEWEST_FIRST,
                    Sort.by(Direction.DESC, UPDATED_AT),
                    "updatedAt,asc",
                    Sort.by(Direction.ASC, UPDATED_AT),
                    "name,asc",
                    Sort.by(Direction.ASC, NAME));

    private StudioSorts() {}

    public static Sort forProblems(String value) {
        return parse(value, PROBLEM_SORTS);
    }

    public static Sort forDatasets(String value) {
        return parse(value, DATASET_SORTS);
    }

    private static Sort parse(String value, Map<String, Sort> allowed) {
        Sort sort = allowed.get(value);
        if (sort == null) {
            throw new InvalidFieldException("sort", "Cách sắp xếp không hợp lệ");
        }
        return sort;
    }
}
