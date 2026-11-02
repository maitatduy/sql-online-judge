package com.sqljudge.judge.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

final class ResultPreview {

    static final int MAX_ROWS = 100;
    private static final int MAX_CELL_CHARS = 500;
    private static final String ELLIPSIS = "…";

    private ResultPreview() {}

    static List<List<Object>> rowsOf(List<List<Object>> rows) {
        return rows.stream().limit(MAX_ROWS).map(ResultPreview::rowOf).toList();
    }

    private static List<Object> rowOf(List<Object> row) {
        List<Object> preview = new ArrayList<>(row.size());
        for (Object value : row) {
            preview.add(cellOf(value));
        }
        return Collections.unmodifiableList(preview);
    }

    private static Object cellOf(Object value) {
        if (value instanceof byte[] bytes) {
            return shortened("0x" + HexFormat.of().formatHex(bytes));
        }
        if (value instanceof CharSequence text) {
            return shortened(text.toString());
        }
        return value;
    }

    private static String shortened(String text) {
        return text.length() > MAX_CELL_CHARS ? text.substring(0, MAX_CELL_CHARS) + ELLIPSIS : text;
    }
}
