package com.sqljudge.judge.job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

final class ResultPreview {

    private static final int MAX_ROWS = 100;
    private static final int MAX_CELL_CHARS = 500;
    private static final int NULL_CHARS = 4;
    private static final String ELLIPSIS = "…";

    private int remainingChars;

    ResultPreview(int budgetChars) {
        this.remainingChars = budgetChars;
    }

    List<List<Object>> take(List<List<Object>> rows) {
        List<List<Object>> preview = new ArrayList<>();
        for (List<Object> row : rows) {
            if (preview.size() == MAX_ROWS) {
                break;
            }
            List<Object> cells = rowOf(row);
            int rowChars = charsOf(cells);
            if (rowChars > remainingChars) {
                break;
            }
            remainingChars -= rowChars;
            preview.add(cells);
        }
        return preview;
    }

    private static List<Object> rowOf(List<Object> row) {
        List<Object> cells = new ArrayList<>(row.size());
        for (Object value : row) {
            cells.add(cellOf(value));
        }
        return Collections.unmodifiableList(cells);
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

    private static int charsOf(List<Object> cells) {
        int chars = 0;
        for (Object cell : cells) {
            chars += cell == null ? NULL_CHARS : String.valueOf(cell).length();
        }
        return chars;
    }
}
