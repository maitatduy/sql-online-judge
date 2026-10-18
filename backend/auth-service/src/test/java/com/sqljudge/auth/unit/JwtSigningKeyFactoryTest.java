package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.config.JwtProperties;
import com.sqljudge.auth.config.JwtSigningKeyFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JwtSigningKeyFactoryTest {

    @TempDir private Path tempDir;

    @Test
    @DisplayName("Bật sinh khoá tạm thì có cặp khoá RSA kèm kid")
    void create_ephemeral_generatesRsaKeyPair() {
        RSAKey key = JwtSigningKeyFactory.create(new JwtProperties(null, true));

        assertThat(key.isPrivate()).isTrue();
        assertThat(key.getKeyID()).isNotBlank();
    }

    @Test
    @DisplayName("Đọc khoá PKCS#8 từ file PEM, kid ổn định qua các lần khởi động")
    void create_fromPemFile_loadsSameKeyWithStableKid() throws Exception {
        RSAKey source = TestJwts.newKey();
        Path pemFile = writePem(source);

        RSAKey first = JwtSigningKeyFactory.create(new JwtProperties(pemFile.toString(), false));
        RSAKey second = JwtSigningKeyFactory.create(new JwtProperties(pemFile.toString(), false));

        assertThat(first.getModulus()).isEqualTo(source.getModulus());
        assertThat(first.isPrivate()).isTrue();
        assertThat(first.getKeyID()).isEqualTo(source.computeThumbprint().toString());
        assertThat(second.getKeyID()).isEqualTo(first.getKeyID());
    }

    @Test
    @DisplayName("Không bật sinh khoá tạm và thiếu đường dẫn khoá thì không khởi động")
    void create_withoutPathOrEphemeral_failsFast() {
        assertThatThrownBy(() -> JwtSigningKeyFactory.create(new JwtProperties(" ", false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PRIVATE_KEY_PATH");
    }

    @Test
    @DisplayName("File khoá không đọc được thì không khởi động")
    void create_unreadableFile_failsFast() {
        String missingFile = tempDir.resolve("missing.pem").toString();

        assertThatThrownBy(() -> JwtSigningKeyFactory.create(new JwtProperties(missingFile, false)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PRIVATE_KEY_PATH");
    }

    private Path writePem(RSAKey key) throws IOException, JOSEException {
        String body =
                Base64.getMimeEncoder(64, "\n".getBytes())
                        .encodeToString(key.toPrivateKey().getEncoded());
        Path file = tempDir.resolve("jwt-private.pem");
        Files.writeString(
                file, "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n");
        return file;
    }
}
