package com.oigit.admin.core.translation;

import java.util.Objects;

public record TranslationKey(String qualifier, String value) {

    public TranslationKey {
        qualifier = Objects.requireNonNullElse(qualifier, "");
        value = Objects.requireNonNull(value, "value");
    }
}
