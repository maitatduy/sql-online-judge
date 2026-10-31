package com.sqljudge.problem.integration;

import com.sqljudge.problem.entity.Dialect;
import com.sqljudge.problem.entity.Difficulty;
import com.sqljudge.problem.entity.ExpectedResult;
import com.sqljudge.problem.entity.LogicalType;
import com.sqljudge.problem.entity.Problem;
import com.sqljudge.problem.entity.ProblemRevision;
import com.sqljudge.problem.entity.ProblemStatus;
import com.sqljudge.problem.entity.ProblemVisibility;
import com.sqljudge.problem.entity.ReferenceSolution;
import com.sqljudge.problem.entity.RevisionColumn;
import com.sqljudge.problem.entity.RevisionTable;
import com.sqljudge.problem.entity.StatementType;
import com.sqljudge.problem.entity.Testcase;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

final class ProblemFixtures {

    static final String HIDDEN_DATA_MARKER = "Phòng bí mật";
    static final String REFERENCE_SOLUTION_MARKER = "SELECT 'dap-an-chuan';";

    private static final UUID AUTHOR_ID = UUID.fromString("0192f3a0-0000-7000-8000-000000000001");
    private static final Instant PUBLISHED_AT = Instant.parse("2026-07-30T00:00:00Z");

    private ProblemFixtures() {}

    static Problem publishedProblem(String slug, int number) {
        Problem problem = new Problem();
        problem.setSlug(slug);
        problem.setNumber(number);
        problem.setTitle("Lương cao nhất mỗi phòng ban");
        problem.setDifficulty(Difficulty.MEDIUM);
        problem.setStatus(ProblemStatus.PUBLISHED);
        problem.setVisibility(ProblemVisibility.PUBLIC);
        problem.setAuthorId(AUTHOR_ID);
        problem.setStatementMarkdown("Tìm nhân viên có **lương cao nhất**.");
        problem.setStarterCode("SELECT *\nFROM employees;");
        problem.setTimeLimitMs(2000);
        problem.setRequiresOrder(true);
        problem.setAllowedStatements(Set.of(StatementType.SELECT, StatementType.WITH));
        problem.setPublishedAt(PUBLISHED_AT);

        ProblemRevision revision = problem.addRevision(1);
        revision.setFrozenAt(PUBLISHED_AT);
        RevisionTable departments = revision.addTable("departments");
        departments.addColumn("id", LogicalType.INTEGER).setPrimaryKey(true);
        departments.addColumn("name", LogicalType.TEXT);
        RevisionTable employees = revision.addTable("employees");
        employees.addColumn("id", LogicalType.INTEGER).setPrimaryKey(true);
        RevisionColumn departmentId = employees.addColumn("department_id", LogicalType.INTEGER);
        departmentId.setNullable(true);
        departmentId.setReferencesTable("departments");
        departmentId.setReferencesColumn("id");
        revision.addReferenceSolution(
                new ReferenceSolution(Dialect.POSTGRESQL, REFERENCE_SOLUTION_MARKER));
        revision.addReferenceSolution(
                new ReferenceSolution(Dialect.MYSQL, REFERENCE_SOLUTION_MARKER));

        Testcase example = revision.addTestcase("Ví dụ trong đề", true);
        example.addData("departments", "id,name\n1,Kỹ thuật\n2,Kinh doanh\n", 2);
        example.addData("employees", "id,department_id\n1,1\n2,1\n3,2\n", 3);
        example.setExpectedResult(
                new ExpectedResult(
                        "[\"department\",\"employee\"]",
                        "[[\"Kỹ thuật\",\"Hoàng Quốc Bảo\"],[\"Kinh doanh\",null]]",
                        2,
                        "x"));
        Testcase hidden = revision.addTestcase("Nhiều người cùng lương", false);
        hidden.addData("departments", "id,name\n1," + HIDDEN_DATA_MARKER + "\n", 1);
        hidden.addData("employees", "id,department_id\n1,1\n", 1);
        hidden.setExpectedResult(
                new ExpectedResult(
                        "[\"department\"]", "[[\"" + HIDDEN_DATA_MARKER + "\"]]", 1, "y"));

        problem.setPublishedRevision(revision);
        return problem;
    }
}
