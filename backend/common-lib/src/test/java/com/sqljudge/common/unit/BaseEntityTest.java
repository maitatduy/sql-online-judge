package com.sqljudge.common.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.common.persistence.BaseEntity;
import com.sqljudge.common.persistence.UuidV7;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BaseEntityTest {

    @Test
    @DisplayName("Hai entity cùng loại cùng id thì bằng nhau và cùng hashCode")
    void equals_sameTypeSameId_isEqual() {
        UUID id = UuidV7.generate();
        SampleEntity first = withId(new SampleEntity(), id);
        SampleEntity second = withId(new SampleEntity(), id);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    @DisplayName("Entity chưa có id chỉ bằng chính nó")
    void equals_transientEntities_areNotEqual() {
        SampleEntity entity = new SampleEntity();

        assertThat(entity).isEqualTo(entity).isNotEqualTo(new SampleEntity()).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Khác loại entity thì không bằng nhau dù trùng id")
    void equals_differentTypeSameId_isNotEqual() {
        UUID id = UuidV7.generate();

        assertThat(withId(new SampleEntity(), id)).isNotEqualTo(withId(new OtherEntity(), id));
    }

    private static <T extends BaseEntity> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    static class SampleEntity extends BaseEntity {}

    static class OtherEntity extends BaseEntity {}
}
