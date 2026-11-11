package com.sqljudge.submission.service;

import com.sqljudge.common.exception.InvalidFieldException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

final class SubmissionCursor {

    private static final int UUID_BYTES = 16;
    private static final String FIELD = "cursor";
    private static final String INVALID_MESSAGE = "Cursor không hợp lệ";

    private SubmissionCursor() {}

    static String encode(UUID id) {
        ByteBuffer buffer = ByteBuffer.allocate(UUID_BYTES);
        buffer.putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    static UUID decode(String cursor) {
        byte[] bytes;
        try {
            bytes = Base64.getUrlDecoder().decode(cursor);
        } catch (IllegalArgumentException exception) {
            throw new InvalidFieldException(FIELD, INVALID_MESSAGE);
        }
        if (bytes.length != UUID_BYTES) {
            throw new InvalidFieldException(FIELD, INVALID_MESSAGE);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
