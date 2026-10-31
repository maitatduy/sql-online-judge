package com.sqljudge.problem.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugGenerator {

    public static final int MAX_BASE_LENGTH = 150;
    private static final String FALLBACK_SLUG = "bai-tap";
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("^-+|-+$");

    private SlugGenerator() {}

    public static String slugOf(String title) {
        String withoutMarks =
                COMBINING_MARKS
                        .matcher(Normalizer.normalize(title, Normalizer.Form.NFD))
                        .replaceAll("")
                        .replace('đ', 'd')
                        .replace('Đ', 'D')
                        .toLowerCase(Locale.ROOT);
        String slug = trimHyphens(NON_ALPHANUMERIC.matcher(withoutMarks).replaceAll("-"));
        if (slug.length() > MAX_BASE_LENGTH) {
            slug = trimHyphens(slug.substring(0, MAX_BASE_LENGTH));
        }
        return slug.isEmpty() ? FALLBACK_SLUG : slug;
    }

    private static String trimHyphens(String value) {
        return EDGE_HYPHENS.matcher(value).replaceAll("");
    }
}
