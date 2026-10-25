package com.airporttransfer.user;

import java.util.Locale;

/** Published inside the transaction that stored the token; the e-mail goes out after commit. */
public record PasswordResetRequested(String email, String fullName, Locale locale, String link) {
}
