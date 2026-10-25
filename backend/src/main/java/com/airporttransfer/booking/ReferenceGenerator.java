package com.airporttransfer.booking;

import java.security.SecureRandom;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/** Human-readable booking references such as {@code MH-7K4Q9XTR}: no 0/O/1/I to avoid phone-call mix-ups. */
@Component
public class ReferenceGenerator {

    static final String PREFIX = "MH-";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LENGTH = 8;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Returns a reference for which {@code taken} is false (the DB unique constraint is the final guard). */
    public String nextFree(Predicate<String> taken) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String candidate = next();
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a free booking reference");
    }
}
