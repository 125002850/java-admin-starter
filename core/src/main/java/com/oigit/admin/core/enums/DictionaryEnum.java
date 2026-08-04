package com.oigit.admin.core.enums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a backend enum whose codes are mirrored as a frontend-facing global
 * dictionary. Enum codes are the business contract; dictionary names are the
 * UI and export display contract.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DictionaryEnum {

    String value();
}
