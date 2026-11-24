package com.sqljudge.ai.client;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.exception.ServiceUnavailableException;
import feign.FeignException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class CommunityServiceFallbackFactory implements FallbackFactory<CommunityServiceClient> {

    @Override
    public CommunityServiceClient create(Throwable cause) {
        return new CommunityServiceClient() {
            @Override
            public CommunityPost post(UUID postId) {
                throw failureOf(postId);
            }

            @Override
            public CommentPage comments(UUID postId) {
                throw failureOf(postId);
            }

            private AppException failureOf(UUID postId) {
                if (cause instanceof FeignException.NotFound) {
                    return new NotFoundException(
                            ErrorCode.POST_NOT_FOUND, "Không tìm thấy bài viết");
                }
                log.warn("community_service_call_failed postId={}", postId, cause);
                return new ServiceUnavailableException(
                        ErrorCode.COMMUNITY_SERVICE_UNAVAILABLE,
                        "Chưa tải được bài viết, vui lòng thử lại sau");
            }
        };
    }
}
