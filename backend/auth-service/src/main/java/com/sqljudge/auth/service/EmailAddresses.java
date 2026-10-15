package com.sqljudge.auth.service;

import java.util.Locale;

final class EmailAddresses {

    private EmailAddresses() {}

    static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
