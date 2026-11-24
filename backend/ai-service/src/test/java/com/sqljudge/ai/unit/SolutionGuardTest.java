package com.sqljudge.ai.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.ai.service.SolutionGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolutionGuardTest {

    @Test
    @DisplayName("Khối code có cả SELECT và FROM bị coi là lời giải trọn vẹn")
    void fullQueryBlock_revealsSolution() {
        assertThat(SolutionGuard.revealsSolution("```sql\nselect name\nFROM users;\n```")).isTrue();
        assertThat(SolutionGuard.revealsSolution("```\nSELECT 1 FROM dual\n```")).isTrue();
    }

    @Test
    @DisplayName("Mệnh đề ngắn, chữ ngoài khối code hay từ ghép không bị chặn")
    void hintsAndProse_areAllowed() {
        assertThat(SolutionGuard.revealsSolution("```sql\nGROUP BY department_id\n```")).isFalse();
        assertThat(SolutionGuard.revealsSolution("Dùng SELECT ... FROM rồi GROUP BY.")).isFalse();
        assertThat(SolutionGuard.revealsSolution("```sql\nselected_from_x\n```")).isFalse();
        assertThat(SolutionGuard.revealsSolution("Không có code")).isFalse();
    }
}
