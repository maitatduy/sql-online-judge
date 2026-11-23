package com.sqljudge.contest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.contest.standings.StandingsStore;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class StandingsStoreTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final StandingsStore store = new StandingsStore(redisTemplate, new ObjectMapper());

    @Test
    @DisplayName("Không xoá được khoá khi Redis lỗi thì chỉ ghi log, không ném lỗi")
    void discardQuietly_redisDown_doesNotThrow() {
        when(redisTemplate.delete(any(Collection.class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> store.discardQuietly(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Contest chưa có bảng trên Redis thì kích thước là 0 và trang rỗng")
    void emptyStandings_haveNoRows() {
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSet = mock(ZSetOperations.class);
        when(redisTemplate.opsForZSet()).thenReturn(zSet);
        when(zSet.zCard(anyString())).thenReturn(null);
        when(zSet.reverseRangeWithScores(anyString(), anyLong(), anyLong())).thenReturn(null);

        assertThat(store.size(UUID.randomUUID())).isZero();
        assertThat(store.page(UUID.randomUUID(), 0, 50)).isEmpty();
    }
}
