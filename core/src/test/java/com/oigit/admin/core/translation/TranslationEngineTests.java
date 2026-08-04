package com.oigit.admin.core.translation;

import com.oigit.admin.core.web.PageResult;
import com.oigit.admin.core.web.R;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationEngineTests {

    @Test
    void translates_nested_page_with_one_provider_call_and_deduplicated_keys() {
        CountingProvider provider = new CountingProvider();
        TranslationEngine engine = new TranslationEngine(List.of(provider));
        AuditRow first = new AuditRow(7L, 8L);
        AuditRow second = new AuditRow(7L, 9L);

        engine.translate(R.ok(new PageResult<>(List.of(first, second), 2)), TranslationScene.WEB_RESPONSE);

        assertThat(provider.calls).isEqualTo(1);
        assertThat(provider.requested).containsExactlyInAnyOrder(
                new TranslationKey("", "7"),
                new TranslationKey("", "8"),
                new TranslationKey("", "9")
        );
        assertThat(first.createByName).isEqualTo("user-7");
        assertThat(first.updateByName).isEqualTo("user-8");
        assertThat(second.createByName).isEqualTo("user-7");
        assertThat(second.updateByName).isEqualTo("user-9");
    }

    @Test
    void honors_scene_and_source_fallback() {
        CountingProvider provider = new CountingProvider();
        TranslationEngine engine = new TranslationEngine(List.of(provider));
        ExportOnlyRow row = new ExportOnlyRow("unknown");

        engine.translate(row, TranslationScene.WEB_RESPONSE);
        assertThat(provider.calls).isZero();
        assertThat(row.label).isNull();

        engine.translate(row, TranslationScene.EXPORT);
        assertThat(provider.calls).isEqualTo(1);
        assertThat(row.label).isEqualTo("unknown");
    }

    private static final class CountingProvider implements TranslationProvider {
        private int calls;
        private Set<TranslationKey> requested = Set.of();

        @Override
        public String type() {
            return TranslationTypes.USER_NAME;
        }

        @Override
        public Map<TranslationKey, String> translate(Set<TranslationKey> keys) {
            calls++;
            requested = keys;
            Map<TranslationKey, String> result = new LinkedHashMap<>();
            for (TranslationKey key : keys) {
                if (!"unknown".equals(key.value())) {
                    result.put(key, "user-" + key.value());
                }
            }
            return result;
        }
    }

    private static final class AuditRow {
        @Translate(type = TranslationTypes.USER_NAME, targetField = "createByName")
        private final Long createById;
        private String createByName;
        @Translate(type = TranslationTypes.USER_NAME, targetField = "updateByName")
        private final Long updateById;
        private String updateByName;

        private AuditRow(Long createById, Long updateById) {
            this.createById = createById;
            this.updateById = updateById;
        }
    }

    private static final class ExportOnlyRow {
        @Translate(
                type = TranslationTypes.USER_NAME,
                targetField = "label",
                scenes = TranslationScene.EXPORT,
                missing = MissingTranslationPolicy.SOURCE
        )
        private final String code;
        private String label;

        private ExportOnlyRow(String code) {
            this.code = code;
        }
    }
}
