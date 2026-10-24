package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.auth.client.AvatarFormat;
import com.sqljudge.auth.client.AvatarUpload;
import com.sqljudge.auth.service.AvatarUploads;
import com.sqljudge.common.exception.InvalidFieldException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AvatarUploadsTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

    @Test
    @DisplayName("Nhận PNG, JPEG, WEBP theo magic bytes, không tin Content-Type gửi lên")
    void read_knownImages_detectsFormatFromBytes() {
        assertThat(AvatarUploads.read(file("anh.bin", "application/octet-stream", PNG)).format())
                .isEqualTo(AvatarFormat.PNG);
        assertThat(AvatarUploads.read(file("anh.png", "image/png", JPEG)).format())
                .isEqualTo(AvatarFormat.JPEG);
        AvatarUpload webp = AvatarUploads.read(file("anh.webp", "image/webp", WEBP));
        assertThat(webp.format()).isEqualTo(AvatarFormat.WEBP);
        assertThat(webp.bytes()).isEqualTo(WEBP);
    }

    @Test
    @DisplayName("File giả đuôi ảnh, GIF hoặc file quá ngắn: 400 trên field file")
    void read_notAnImage_rejects() {
        byte[] html = "<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        byte[] gif = "GIF89a".getBytes(StandardCharsets.US_ASCII);

        assertInvalidFile(file("anh.png", "image/png", html), "Ảnh phải là PNG, JPG hoặc WEBP");
        assertInvalidFile(file("anh.gif", "image/gif", gif), "Ảnh phải là PNG, JPG hoặc WEBP");
        assertInvalidFile(
                file("anh.png", "image/png", new byte[] {(byte) 0x89}),
                "Ảnh phải là PNG, JPG hoặc WEBP");
    }

    @Test
    @DisplayName("Không có file hoặc file rỗng: 400 Chưa chọn ảnh")
    void read_missingOrEmpty_rejects() {
        assertInvalidFile(null, "Chưa chọn ảnh");
        assertInvalidFile(file("anh.png", "image/png", new byte[0]), "Chưa chọn ảnh");
    }

    @Test
    @DisplayName("Ảnh đúng 2 MB được nhận, lớn hơn 2 MB bị từ chối")
    void read_sizeLimit_isTwoMegabytes() {
        byte[] atLimit = Arrays.copyOf(PNG, 2 * 1024 * 1024);
        byte[] overLimit = Arrays.copyOf(PNG, 2 * 1024 * 1024 + 1);

        assertThat(AvatarUploads.read(file("anh.png", "image/png", atLimit)).format())
                .isEqualTo(AvatarFormat.PNG);
        assertInvalidFile(file("anh.png", "image/png", overLimit), "Ảnh tối đa 2 MB");
    }

    private static MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private static void assertInvalidFile(MockMultipartFile file, String message) {
        assertThatThrownBy(() -> AvatarUploads.read(file))
                .isInstanceOfSatisfying(
                        InvalidFieldException.class,
                        rejected -> {
                            assertThat(rejected.getViolation().field()).isEqualTo("file");
                            assertThat(rejected.getViolation().message()).isEqualTo(message);
                        });
    }
}
