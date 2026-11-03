package com.sqljudge.problem.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "answer_check_testcases",
        indexes =
                @Index(
                        name = "uk_answer_check_testcases_check_id_testcase_id",
                        columnList = "check_id, testcase_id",
                        unique = true))
@Getter
@NoArgsConstructor
public class AnswerCheckTestcase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "check_id", nullable = false)
    private AnswerCheck check;

    @Column(name = "testcase_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID testcaseId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "is_passed", nullable = false)
    private boolean isPassed;

    @Column(name = "system_error", length = 64)
    private String systemError;

    @Column(name = "dialects_json", nullable = false, columnDefinition = "JSON")
    private String dialectsJson;

    AnswerCheckTestcase(AnswerCheck check, Testcase testcase, TestcaseCheck result) {
        this.check = check;
        this.testcaseId = testcase.getId();
        this.position = testcase.getPosition() + 1;
        this.isPublic = testcase.isPublic();
        this.isPassed = result.isPassed();
        this.systemError = result.systemError();
        this.dialectsJson = result.dialectsJson();
    }

    public record TestcaseCheck(boolean isPassed, String systemError, String dialectsJson) {}
}
