package com.sqljudge.contest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.contest.service.StandingsStreamService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class StandingsStreamServiceTest {

    private final StandingsStreamService streamService = new StandingsStreamService();
    private final UUID contestId = UUID.randomUUID();

    @Test
    @DisplayName("Nhiều thay đổi trong một nhịp chỉ gửi một sự kiện cho mỗi người xem")
    void changesInOneTick_areSentOnce() {
        RecordingEmitter viewer = new RecordingEmitter();
        streamService.register(contestId, viewer);

        streamService.markChanged(contestId);
        streamService.markChanged(contestId);
        streamService.flush();
        streamService.flush();

        assertThat(viewer.sent).hasSize(1);
    }

    @Test
    @DisplayName("Không có thay đổi thì không gửi gì, contest khác không bị ảnh hưởng")
    void noChanges_sendNothing() {
        RecordingEmitter viewer = new RecordingEmitter();
        streamService.register(contestId, viewer);

        streamService.markChanged(UUID.randomUUID());
        streamService.flush();

        assertThat(viewer.sent).isEmpty();
    }

    @Test
    @DisplayName("Người xem đã đóng kết nối bị bỏ khỏi danh sách, người khác vẫn nhận")
    void closedViewer_isDropped() {
        RecordingEmitter closed = new RecordingEmitter();
        closed.isClosed = true;
        RecordingEmitter open = new RecordingEmitter();
        streamService.register(contestId, closed);
        streamService.register(contestId, open);

        streamService.markChanged(contestId);
        streamService.flush();
        streamService.markChanged(contestId);
        streamService.flush();

        assertThat(open.sent).hasSize(2);
        assertThat(streamService.viewerCount(contestId)).isEqualTo(1);
    }

    private static final class RecordingEmitter extends SseEmitter {

        private final List<Set<ResponseBodyEmitter.DataWithMediaType>> sent = new ArrayList<>();
        private boolean isClosed;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (isClosed) {
                throw new IOException("closed");
            }
            sent.add(builder.build());
        }
    }
}
