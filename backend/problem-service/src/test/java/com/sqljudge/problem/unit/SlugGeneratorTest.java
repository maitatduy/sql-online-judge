package com.sqljudge.problem.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.problem.service.SlugGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SlugGeneratorTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("Sinh slug kebab-case không dấu từ tiêu đề tiếng Việt")
    @CsvSource(
            delimiter = '|',
            value = {
                "Lương cao nhất mỗi phòng ban | luong-cao-nhat-moi-phong-ban",
                "Đếm ĐƠN HÀNG theo trạng thái | dem-don-hang-theo-trang-thai",
                "  Top 3 sản phẩm -- mỗi tháng!  | top-3-san-pham-moi-thang",
                "SELECT * FROM users; | select-from-users",
                "!!! | bai-tap"
            })
    void slugOf_vietnameseTitle_returnsAsciiKebabCase(String title, String expected) {
        assertThat(SlugGenerator.slugOf(title)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "dài {0} ký tự")
    @DisplayName("Cắt slug dài về tối đa 150 ký tự, không để gạch nối ở cuối")
    @CsvSource({"200", "151"})
    void slugOf_longTitle_isTruncatedWithoutTrailingHyphen(int length) {
        String slug = SlugGenerator.slugOf("ab ".repeat(length));

        assertThat(slug).hasSizeLessThanOrEqualTo(150).doesNotEndWith("-");
    }
}
