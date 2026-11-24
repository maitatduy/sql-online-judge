package com.sqljudge.ai.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SolutionGuard {

    public static final String BLOCKED_REPLY =
            "Mình không đưa câu truy vấn hoàn chỉnh khi bạn chưa AC bài này. Hãy hỏi gợi ý cho"
                    + " bước bạn đang vướng, mình sẽ giúp từng phần nhé.";

    private static final Pattern CODE_BLOCK = Pattern.compile("```[^\\n]*\\n([\\s\\S]*?)```");
    private static final Pattern SELECT = Pattern.compile("\\bselect\\b");
    private static final Pattern FROM = Pattern.compile("\\bfrom\\b");

    private SolutionGuard() {}

    public static boolean revealsSolution(String reply) {
        Matcher blocks = CODE_BLOCK.matcher(reply);
        while (blocks.find()) {
            String code = blocks.group(1).toLowerCase(Locale.ROOT);
            if (SELECT.matcher(code).find() && FROM.matcher(code).find()) {
                return true;
            }
        }
        return false;
    }
}
