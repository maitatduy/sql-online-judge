package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.oauth.OAuthProfiles;
import com.sqljudge.auth.service.OAuthProfile;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthProfilesTest {

    @Test
    @DisplayName("GitHub: id số, login làm gợi ý username, email lấy từ /user/emails")
    void github_mapsLoginAndVerifiedEmail() {
        OAuthProfile profile =
                OAuthProfiles.from(
                        "github",
                        Map.of(
                                "id", 583231,
                                "login", "Octo-Cat",
                                "name", "Octo Cat",
                                "email", "octo@gmail.com",
                                "email_verified", true));

        assertThat(profile)
                .isEqualTo(
                        new OAuthProfile(
                                SignupProvider.GITHUB,
                                "583231",
                                "octo@gmail.com",
                                true,
                                "Octo-Cat",
                                "Octo Cat"));
    }

    @Test
    @DisplayName("Google: sub làm id, username gợi ý từ phần trước @ của email")
    void google_mapsSubjectAndEmailLocalPart() {
        OAuthProfile profile =
                OAuthProfiles.from(
                        "google",
                        Map.of(
                                "sub", "109876543210",
                                "email", "Minh.Anh@gmail.com",
                                "email_verified", true,
                                "name", "Nguyễn Minh Anh"));

        assertThat(profile)
                .isEqualTo(
                        new OAuthProfile(
                                SignupProvider.GOOGLE,
                                "109876543210",
                                "Minh.Anh@gmail.com",
                                true,
                                "Minh.Anh",
                                "Nguyễn Minh Anh"));
    }

    @Test
    @DisplayName("Google trả email_verified dạng chuỗi hoặc thiếu: chỉ \"true\" mới được tin")
    void google_emailVerifiedAsStringOrMissing() {
        OAuthProfile asString =
                OAuthProfiles.from(
                        "google", Map.of("sub", "1", "email", "a@b.dev", "email_verified", "true"));
        OAuthProfile missing = OAuthProfiles.from("google", Map.of("sub", "2", "email", "a@b.dev"));

        assertThat(asString.isEmailVerified()).isTrue();
        assertThat(missing.isEmailVerified()).isFalse();
    }
}
