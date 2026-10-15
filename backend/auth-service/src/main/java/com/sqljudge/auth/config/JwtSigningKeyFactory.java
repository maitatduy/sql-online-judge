package com.sqljudge.auth.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public final class JwtSigningKeyFactory {

    private static final int EPHEMERAL_KEY_SIZE = 2048;
    private static final String PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_FOOTER = "-----END PRIVATE KEY-----";
    private static final String INVALID_KEY =
            "JWT_PRIVATE_KEY_PATH must point to a readable PKCS#8 PEM RSA private key";

    private JwtSigningKeyFactory() {}

    public static RSAKey create(JwtProperties properties) {
        String keyPath = properties.privateKeyPath();
        if (keyPath != null && !keyPath.isBlank()) {
            return load(Path.of(keyPath));
        }
        if (properties.generateEphemeralKey()) {
            return generate();
        }
        throw new IllegalStateException(INVALID_KEY);
    }

    private static RSAKey generate() {
        try {
            return new RSAKeyGenerator(EPHEMERAL_KEY_SIZE).keyIDFromThumbprint(true).generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Cannot generate ephemeral JWT key", exception);
        }
    }

    private static RSAKey load(Path pemFile) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privateKey =
                    (RSAPrivateCrtKey)
                            keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodePem(pemFile)));
            RSAPublicKey publicKey =
                    (RSAPublicKey)
                            keyFactory.generatePublic(
                                    new RSAPublicKeySpec(
                                            privateKey.getModulus(),
                                            privateKey.getPublicExponent()));
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyIDFromThumbprint()
                    .build();
        } catch (IOException
                | GeneralSecurityException
                | JOSEException
                | IllegalArgumentException
                | ClassCastException exception) {
            throw new IllegalStateException(INVALID_KEY, exception);
        }
    }

    private static byte[] decodePem(Path pemFile) throws IOException {
        String body =
                Files.readString(pemFile)
                        .replace(PEM_HEADER, "")
                        .replace(PEM_FOOTER, "")
                        .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
