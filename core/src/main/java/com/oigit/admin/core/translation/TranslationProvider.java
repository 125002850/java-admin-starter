package com.oigit.admin.core.translation;

import java.util.Map;
import java.util.Set;

/**
 * A translation provider is batch-only. One invocation receives every unique
 * key for this provider in the current translation batch.
 */
public interface TranslationProvider {

    String type();

    Map<TranslationKey, String> translate(Set<TranslationKey> keys);
}
