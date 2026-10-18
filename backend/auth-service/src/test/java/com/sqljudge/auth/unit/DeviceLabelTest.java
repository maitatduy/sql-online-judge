package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.service.DeviceLabel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeviceLabelTest {

    @ParameterizedTest(name = "{1} trên {2}")
    @CsvSource(
            delimiter = '|',
            value = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                        + " Chrome/129.0.0.0 Safari/537.36|Chrome|Windows",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                        + " Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0|Edge|Windows",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                        + " Chrome/129.0.0.0 Safari/537.36 OPR/114.0.0.0|Opera|Windows",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6) AppleWebKit/605.1.15 (KHTML, like"
                        + " Gecko) Version/18.0 Safari/605.1.15|Safari|macOS",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15"
                    + " (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1|Safari|iPhone",
                "Mozilla/5.0 (iPad; CPU OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like"
                        + " Gecko) CriOS/129.0 Mobile/15E148 Safari/604.1|Chrome|iPad",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko)"
                        + " Chrome/129.0.0.0 Mobile Safari/537.36|Chrome|Android",
                "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:131.0) Gecko/20100101"
                        + " Firefox/131.0|Firefox|Linux",
                "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko)"
                        + " Chrome/129.0.0.0 Safari/537.36|Chrome|ChromeOS"
            })
    @DisplayName("Nhận ra trình duyệt và hệ điều hành phổ biến")
    void of_knownUserAgent_namesBrowserAndOs(String userAgent, String browser, String os) {
        DeviceLabel label = DeviceLabel.of(userAgent);

        assertThat(label.browser()).isEqualTo(browser);
        assertThat(label.os()).isEqualTo(os);
    }

    @Test
    @DisplayName("Không có hoặc không nhận ra User-Agent thì để trống cho giao diện tự ghi")
    void of_missingOrUnknown_isUnknown() {
        assertThat(DeviceLabel.of(null)).isEqualTo(new DeviceLabel(null, null));
        assertThat(DeviceLabel.of("curl/8.4.0")).isEqualTo(new DeviceLabel(null, null));
    }
}
