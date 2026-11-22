package com.sqljudge.contest.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.exception.ServiceUnavailableException;
import com.sqljudge.contest.entity.ContestProblemInfo;
import feign.FeignException;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ProblemServiceFallbackFactory implements FallbackFactory<ProblemServiceClient> {

    @Override
    public ProblemServiceClient create(Throwable cause) {
        return new ProblemServiceClient() {
            @Override
            public List<ContestProblemInfo> contestReady(List<UUID> problemIds) {
                log.warn("problem_service_call_failed problemCount={}", problemIds.size(), cause);
                throw unavailable("Chưa kiểm tra được bài tập, vui lòng thử lại sau");
            }

            @Override
            public ContestStatement contestStatement(UUID problemId) {
                if (cause instanceof FeignException.NotFound) {
                    throw new NotFoundException(
                            ErrorCode.PROBLEM_NOT_FOUND, "Không tìm thấy bài tập");
                }
                log.warn("problem_service_call_failed problemId={}", problemId, cause);
                throw unavailable("Chưa tải được đề bài, vui lòng thử lại sau");
            }
        };
    }

    private static ServiceUnavailableException unavailable(String message) {
        return new ServiceUnavailableException(ErrorCode.PROBLEM_SERVICE_UNAVAILABLE, message);
    }
}
