package com.airporttransfer.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReferenceGeneratorTest {

    private final ReferenceGenerator generator = new ReferenceGenerator();

    @Test
    void referencesAreReadableAndAvoidAmbiguousCharacters() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            String ref = generator.next();
            assertThat(ref).matches("MH-[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
            seen.add(ref);
        }
        assertThat(seen).hasSize(2000);
    }

    @Test
    void nextFreeSkipsTakenReferences() {
        int[] calls = {0};
        String ref = generator.nextFree(candidate -> ++calls[0] < 3);

        assertThat(ref).startsWith("MH-");
        assertThat(calls[0]).isEqualTo(3);
    }

    @Test
    void nextFreeGivesUpWhenEverythingIsTaken() {
        assertThatThrownBy(() -> generator.nextFree(candidate -> true)).isInstanceOf(IllegalStateException.class);
    }
}
