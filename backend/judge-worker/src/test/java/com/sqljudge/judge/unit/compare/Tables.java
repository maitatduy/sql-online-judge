package com.sqljudge.judge.unit.compare;

import com.sqljudge.judge.compare.NormalizedTable;
import com.sqljudge.judge.compare.ResultNormalizer;
import com.sqljudge.judge.compare.ResultTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class Tables {

    private static final ResultNormalizer NORMALIZER = new ResultNormalizer();

    private Tables() {}

    static List<String> columns(String... names) {
        return List.of(names);
    }

    static List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    @SafeVarargs
    static ResultTable raw(List<String> columns, List<Object>... rows) {
        return new ResultTable(columns, List.of(rows));
    }

    @SafeVarargs
    static NormalizedTable table(List<String> columns, List<Object>... rows) {
        return NORMALIZER.normalize(raw(columns, rows));
    }

    static NormalizedTable repeated(List<String> columns, int count) {
        List<List<Object>> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(row(i, "row-" + i));
        }
        return NORMALIZER.normalize(new ResultTable(columns, rows));
    }
}
