package com.sqljudge.auth.service;

import com.sqljudge.auth.client.AvatarFormat;
import com.sqljudge.auth.client.AvatarUpload;
import com.sqljudge.common.exception.InvalidFieldException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public final class AvatarUploads {

    static final long MAX_BYTES = 2L * 1024 * 1024;

    private static final String FIELD = "file";
    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'
    };
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final int WEBP_MARKER_OFFSET = 8;

    private AvatarUploads() {}

    public static AvatarUpload read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFieldException(FIELD, "Chưa chọn ảnh");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidFieldException(FIELD, "Ảnh tối đa 2 MB");
        }
        byte[] bytes = bytesOf(file);
        AvatarFormat format =
                formatOf(bytes)
                        .orElseThrow(
                                () ->
                                        new InvalidFieldException(
                                                FIELD, "Ảnh phải là PNG, JPG hoặc WEBP"));
        return new AvatarUpload(bytes, format);
    }

    private static Optional<AvatarFormat> formatOf(byte[] bytes) {
        if (startsWith(bytes, 0, PNG_SIGNATURE)) {
            return Optional.of(AvatarFormat.PNG);
        }
        if (startsWith(bytes, 0, JPEG_SIGNATURE)) {
            return Optional.of(AvatarFormat.JPEG);
        }
        if (startsWith(bytes, 0, RIFF) && startsWith(bytes, WEBP_MARKER_OFFSET, WEBP)) {
            return Optional.of(AvatarFormat.WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] bytes, int offset, byte[] prefix) {
        return bytes.length >= offset + prefix.length
                && Arrays.equals(bytes, offset, offset + prefix.length, prefix, 0, prefix.length);
    }

    private static byte[] bytesOf(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
