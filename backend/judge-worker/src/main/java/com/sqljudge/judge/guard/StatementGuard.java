package com.sqljudge.judge.guard;

import com.sqljudge.judge.compare.SanitizedMessage;
import com.sqljudge.judge.compare.Verdict;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.ParenthesedStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.delete.ParenthesedDelete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.insert.ParenthesedInsert;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.statement.update.ParenthesedUpdate;
import net.sf.jsqlparser.statement.update.Update;

public final class StatementGuard {

    private static final Pattern COMMENTS = Pattern.compile("(?s)/\\*.*?\\*/|--[^\\n]*");
    private static final Pattern SEPARATORS = Pattern.compile("[;\\s]");
    private static final Pattern LEADING_WORD = Pattern.compile("^[\\s;(]*([A-Za-z]+)");
    private static final Set<String> STATEMENT_KEYWORDS =
            Set.of(
                    "ALTER",
                    "ANALYZE",
                    "ATTACH",
                    "BEGIN",
                    "CALL",
                    "CHECKPOINT",
                    "CLUSTER",
                    "COMMENT",
                    "COMMIT",
                    "COPY",
                    "CREATE",
                    "DEALLOCATE",
                    "DECLARE",
                    "DELETE",
                    "DETACH",
                    "DO",
                    "DROP",
                    "EXEC",
                    "EXECUTE",
                    "EXPLAIN",
                    "FLUSH",
                    "GRANT",
                    "HANDLER",
                    "IMPORT",
                    "INSERT",
                    "INSTALL",
                    "KILL",
                    "LISTEN",
                    "LOAD",
                    "LOCK",
                    "MERGE",
                    "NOTIFY",
                    "OPTIMIZE",
                    "PRAGMA",
                    "PREPARE",
                    "REINDEX",
                    "RENAME",
                    "REPAIR",
                    "REPLACE",
                    "RESET",
                    "REVOKE",
                    "ROLLBACK",
                    "SAVEPOINT",
                    "SECURITY",
                    "SELECT",
                    "SET",
                    "SHOW",
                    "SHUTDOWN",
                    "TRUNCATE",
                    "UNINSTALL",
                    "UPDATE",
                    "UPSERT",
                    "USE",
                    "VACUUM",
                    "WITH");
    private static final GuardDecision NO_STATEMENT =
            new GuardDecision.Rejected(Verdict.RE, new SanitizedMessage("Chưa có câu lệnh nào"));

    private enum Check {
        ALLOWED,
        UNVERIFIED,
        FORBIDDEN
    }

    public GuardDecision check(String sql, GuardPolicy policy) {
        String code = COMMENTS.matcher(sql).replaceAll(" ");
        if (SEPARATORS.matcher(code).replaceAll("").isEmpty()) {
            return NO_STATEMENT;
        }
        Set<StatementType> allowed = policy.allowedStatements();
        List<Check> checks = checksOf(sql, allowed);
        boolean hasForbidden =
                checks.contains(Check.FORBIDDEN)
                        || checks.contains(Check.UNVERIFIED)
                                && hasForbiddenLeadingWord(code, allowed);
        if (hasForbidden) {
            return forbidden("Bài này chỉ cho phép " + namesOf(allowed));
        }
        if (checks.size() > policy.maxStatements()) {
            return forbidden(tooManyStatementsMessage(policy.maxStatements()));
        }
        if (checks.contains(Check.UNVERIFIED)) {
            return new GuardDecision.Unverified();
        }
        return new GuardDecision.Allowed();
    }

    private static List<Check> checksOf(String sql, Set<StatementType> allowed) {
        try {
            Statements parsed =
                    CCJSqlParserUtil.parseStatements(
                            sql, parser -> parser.withUnsupportedStatements());
            return parsed.stream().map(statement -> checkOf(statement, allowed)).toList();
        } catch (JSQLParserException exception) {
            return List.of(Check.UNVERIFIED);
        }
    }

    private static Check checkOf(Statement statement, Set<StatementType> allowed) {
        if (statement instanceof UnsupportedStatement) {
            return Check.UNVERIFIED;
        }
        return isAllowed(statement, allowed) ? Check.ALLOWED : Check.FORBIDDEN;
    }

    private static boolean hasForbiddenLeadingWord(String code, Set<StatementType> allowed) {
        return Arrays.stream(code.split(";"))
                .map(StatementGuard::leadingWord)
                .anyMatch(
                        keyword ->
                                STATEMENT_KEYWORDS.contains(keyword)
                                        && allowed.stream()
                                                .noneMatch(type -> type.name().equals(keyword)));
    }

    private static String leadingWord(String segment) {
        Matcher matcher = LEADING_WORD.matcher(segment);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : "";
    }

    private static boolean isAllowed(Statement statement, Set<StatementType> allowed) {
        if (statement instanceof PlainSelect select && writesInto(select)) {
            return false;
        }
        if (statement instanceof Select select) {
            StatementType type =
                    hasItems(select.getWithItemsList()) ? StatementType.WITH : StatementType.SELECT;
            return allowed.contains(type) && withItemsAllowed(select.getWithItemsList(), allowed);
        }
        if (statement instanceof Insert insert) {
            return allowed.contains(StatementType.INSERT)
                    && withItemsAllowed(insert.getWithItemsList(), allowed);
        }
        if (statement instanceof Update update) {
            return allowed.contains(StatementType.UPDATE)
                    && withItemsAllowed(update.getWithItemsList(), allowed);
        }
        if (statement instanceof Delete delete) {
            return allowed.contains(StatementType.DELETE)
                    && withItemsAllowed(delete.getWithItemsList(), allowed);
        }
        return false;
    }

    private static boolean writesInto(PlainSelect select) {
        return hasItems(select.getIntoTables()) || select.getIntoTempTable() != null;
    }

    private static boolean withItemsAllowed(
            List<WithItem<?>> withItems, Set<StatementType> allowed) {
        return withItems == null
                || withItems.stream()
                        .map(WithItem::getParenthesedStatement)
                        .allMatch(inner -> isAllowedInWith(inner, allowed));
    }

    private static boolean isAllowedInWith(ParenthesedStatement inner, Set<StatementType> allowed) {
        if (inner instanceof ParenthesedSelect) {
            return true;
        }
        if (inner instanceof ParenthesedInsert) {
            return allowed.contains(StatementType.INSERT);
        }
        if (inner instanceof ParenthesedUpdate) {
            return allowed.contains(StatementType.UPDATE);
        }
        if (inner instanceof ParenthesedDelete) {
            return allowed.contains(StatementType.DELETE);
        }
        return false;
    }

    private static boolean hasItems(List<?> items) {
        return items != null && !items.isEmpty();
    }

    private static String namesOf(Set<StatementType> allowed) {
        return allowed.stream().sorted().map(Enum::name).collect(Collectors.joining(", "));
    }

    private static String tooManyStatementsMessage(int maxStatements) {
        return maxStatements == 1
                ? "Bài này chỉ cho phép 1 câu lệnh"
                : "Bài này cho phép tối đa " + maxStatements + " câu lệnh";
    }

    private static GuardDecision forbidden(String message) {
        return new GuardDecision.Rejected(Verdict.FS, new SanitizedMessage(message));
    }
}
