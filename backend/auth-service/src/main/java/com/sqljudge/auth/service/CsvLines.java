package com.sqljudge.auth.service;

import java.util.List;

final class CsvLines {

    private static final String SEPARATOR = ",";
    private static final String LINE_END = "\n";
    private static final String FORMULA_GUARD = "'";
    private static final String FORMULA_PREFIXES = "=+-@\t\r";
    private static final String QUOTE = "\"";

    private CsvLines() {}

    static String line(List<String> cells) {
        return String.join(SEPARATOR, cells.stream().map(CsvLines::cell).toList()) + LINE_END;
    }

    private static String cell(String value) {
        String guarded = startsWithFormula(value) ? FORMULA_GUARD + value : value;
        if (!needsQuoting(guarded)) {
            return guarded;
        }
        return QUOTE + guarded.replace(QUOTE, QUOTE + QUOTE) + QUOTE;
    }

    private static boolean startsWithFormula(String value) {
        return !value.isEmpty() && FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0;
    }

    private static boolean needsQuoting(String value) {
        return value.contains(SEPARATOR)
                || value.contains(QUOTE)
                || value.contains("\n")
                || value.contains("\r");
    }
}
