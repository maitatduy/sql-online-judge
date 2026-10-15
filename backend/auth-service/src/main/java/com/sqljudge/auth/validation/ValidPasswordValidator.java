package com.sqljudge.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    static final int MIN_LENGTH = 8;
    static final int MAX_BCRYPT_BYTES = 72;

    private static final String COMMON_PASSWORDS_RESOURCE = "/common-passwords.txt";
    private static final Set<String> COMMON_PASSWORDS = loadCommonPasswords();

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        if (password.length() < MIN_LENGTH) {
            return reject(context, "Mật khẩu cần ít nhất " + MIN_LENGTH + " ký tự");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            return reject(context, "Mật khẩu quá dài");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            return reject(context, "Mật khẩu này quá phổ biến, hãy chọn mật khẩu khác");
        }
        return true;
    }

    private static boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    private static Set<String> loadCommonPasswords() {
        try (InputStream stream =
                        ValidPasswordValidator.class.getResourceAsStream(
                                COMMON_PASSWORDS_RESOURCE);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
