package com.oigit.admin.core.translation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a source field that must be batch translated into another field.
 * Providers must implement a batch API; per-row lookups are not supported.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Translate {

    String type();

    String targetField();

    String qualifier() default "";

    TranslationScene[] scenes() default {TranslationScene.WEB_RESPONSE, TranslationScene.EXPORT};

    MissingTranslationPolicy missing() default MissingTranslationPolicy.NULL;
}
