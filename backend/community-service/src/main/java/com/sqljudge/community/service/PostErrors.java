package com.sqljudge.community.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;

final class PostErrors {

    private PostErrors() {}

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.POST_NOT_FOUND, "Không tìm thấy bài viết");
    }
}
