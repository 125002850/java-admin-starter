package com.oigit.admin.dict.domain.service;

import java.util.Set;

/** Domain policy for dictionaries whose codes are owned by backend enums. */
public interface EnumDictionaryPolicy {

    boolean isEnumDictionary(String dictTypeCode);

    Set<String> expectedCodes(String dictTypeCode);

    static EnumDictionaryPolicy none() {
        return new EnumDictionaryPolicy() {
            @Override
            public boolean isEnumDictionary(String dictTypeCode) {
                return false;
            }

            @Override
            public Set<String> expectedCodes(String dictTypeCode) {
                return Set.of();
            }
        };
    }
}
